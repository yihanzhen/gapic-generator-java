// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.api.generator.engine.writer;

import com.google.api.generator.engine.ast.AnnotationNode;
import com.google.api.generator.engine.ast.AssignmentExpr;
import com.google.api.generator.engine.ast.AstNodeVisitor;
import com.google.api.generator.engine.ast.ClassDefinition;
import com.google.api.generator.engine.ast.Expr;
import com.google.api.generator.engine.ast.ExprStatement;
import com.google.api.generator.engine.ast.ForStatement;
import com.google.api.generator.engine.ast.IdentifierNode;
import com.google.api.generator.engine.ast.IfStatement;
import com.google.api.generator.engine.ast.MethodDefinition;
import com.google.api.generator.engine.ast.MethodInvocationExpr;
import com.google.api.generator.engine.ast.Reference;
import com.google.api.generator.engine.ast.ScopeNode;
import com.google.api.generator.engine.ast.Statement;
import com.google.api.generator.engine.ast.TryCatchStatement;
import com.google.api.generator.engine.ast.TypeNode;
import com.google.api.generator.engine.ast.ValueExpr;
import com.google.api.generator.engine.ast.VariableExpr;
import com.google.api.generator.engine.ast.WhileStatement;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class ImportWriterVisitor implements AstNodeVisitor {
  private static final String NEWLINE = "\n";
  private static final String PKG_JAVA_LANG = "java.lang";

  private final Set<String> staticImports = new TreeSet<>();
  private final Set<String> imports = new TreeSet<>();

  private final String currentPackage;

  public ImportWriterVisitor(String currentPackage) {
    this.currentPackage = currentPackage;
  }

  public void clear() {
    staticImports.clear();
    imports.clear();
  }

  public String write() {
    StringBuffer sb = new StringBuffer();
    if (!staticImports.isEmpty()) {
      sb.append(
          String.format("import static %s;\n\n", String.join(";\nimport static ", staticImports)));
    }
    if (!imports.isEmpty()) {
      sb.append(String.format("import %s;\n\n", String.join(";\nimport ", imports)));
    }
    return sb.toString();
  }

  @Override
  public void visit(IdentifierNode identifier) {
    // Nothing to do.
  }

  @Override
  public void visit(TypeNode type) {
    if (!TypeNode.isReferenceType(type)) {
      return;
    }

    List<Reference> refs = new ArrayList<>(type.reference().generics());
    refs.add(type.reference());
    references(refs);
  }

  @Override
  public void visit(ScopeNode scope) {
    // Nothing to do.
  }

  @Override
  public void visit(AnnotationNode annotation) {
    annotation.type().accept(this);
  }

  /** =============================== EXPRESSIONS =============================== */
  @Override
  public void visit(ValueExpr valueExpr) {
    valueExpr.type().accept(this);
  }

  @Override
  public void visit(VariableExpr variableExpr) {
    variableExpr.variable().type().accept(this);
  }

  @Override
  public void visit(AssignmentExpr assignmentExpr) {
    assignmentExpr.variableExpr().accept(this);
    assignmentExpr.valueExpr().accept(this);
  }

  @Override
  public void visit(MethodInvocationExpr methodInvocationExpr) {
    methodInvocationExpr.returnType().accept(this);
    if (methodInvocationExpr.exprReferenceExpr() != null) {
      methodInvocationExpr.exprReferenceExpr().accept(this);
    }
    references(methodInvocationExpr.generics());
    expressions(methodInvocationExpr.arguments());
  }

  /** =============================== STATEMENTS =============================== */
  @Override
  public void visit(ExprStatement exprStatement) {
    exprStatement.expression().accept(this);
  }

  @Override
  public void visit(IfStatement ifStatement) {
    ifStatement.conditionExpr().accept(this);
    statements(ifStatement.body());
    for (Map.Entry<Expr, List<Statement>> elseIf : ifStatement.elseIfs().entrySet()) {
      elseIf.getKey().accept(this);
      statements(elseIf.getValue());
    }
    statements(ifStatement.elseBody());
  }

  @Override
  public void visit(ForStatement forStatement) {
    forStatement.localVariableExpr().accept(this);
    forStatement.collectionExpr().accept(this);
    statements(forStatement.body());
  }

  @Override
  public void visit(WhileStatement whileStatement) {
    whileStatement.conditionExpr().accept(this);
    statements(whileStatement.body());
  }

  @Override
  public void visit(TryCatchStatement tryCatchStatement) {
    if (tryCatchStatement.tryResourceExpr() != null) {
      tryCatchStatement.tryResourceExpr().accept(this);
    }

    statements(tryCatchStatement.tryBody());

    Preconditions.checkState(
        !tryCatchStatement.isSampleCode() && tryCatchStatement.catchVariableExpr() != null,
        "Import generation should not be invoked on sample code, but was found when visiting a"
            + " try-catch block");
    tryCatchStatement.catchVariableExpr().accept(this);
    statements(tryCatchStatement.catchBody());
  }

  /** =============================== OTHER =============================== */
  @Override
  public void visit(MethodDefinition methodDefinition) {
    methodDefinition.returnType().accept(this);
    annotations(methodDefinition.annotations());
    types(methodDefinition.throwsExceptions());
    variableExpressions(methodDefinition.arguments());
    statements(methodDefinition.body());
    if (methodDefinition.returnExpr() != null) {
      methodDefinition.returnExpr().accept(this);
    }
  }

  @Override
  public void visit(ClassDefinition classDefinition) {
    annotations(classDefinition.annotations());
    types(classDefinition.implementsTypes());
    if (classDefinition.extendsType() != null) {
      classDefinition.extendsType().accept(this);
    }
    statements(classDefinition.statements());
    for (MethodDefinition method : classDefinition.methods()) {
      method.accept(this);
    }
    for (ClassDefinition nestedClass : classDefinition.nestedClasses()) {
      nestedClass.accept(this);
    }
  }

  /** =============================== PRIVATE HELPERS =============================== */

  private void annotations(Collection<AnnotationNode> annotations) {
    for (AnnotationNode annotation : annotations) {
      annotation.accept(this);
    }
  }

  private void expressions(List<Expr> expressions) {
    for (Expr expr : expressions) {
      expr.accept(this);
    }
  }

  private void variableExpressions(List<VariableExpr> expressions) {
    for (VariableExpr expr : expressions) {
      expr.accept(this);
    }
  }

  private void references(List<Reference> refs) {
    for (Reference ref : refs) {
      Class clazz = ref.clazz();
      // Don't need to import this.
      if (clazz.getPackage().getName().equals(PKG_JAVA_LANG)
          || clazz.getPackage().getName().equals(currentPackage)) {
        continue;
      }

      if (clazz.getEnclosingClass() != null) {
        // This is a static import.
        staticImports.add(clazz.getCanonicalName());
      } else {
        imports.add(clazz.getCanonicalName());
      }

      references(ref.generics());
    }
  }

  private void statements(List<Statement> statements) {
    for (Statement statement : statements) {
      statement.accept(this);
    }
  }

  private void types(Collection<TypeNode> types) {
    for (TypeNode type : types) {
      type.accept(this);
    }
  }
}
