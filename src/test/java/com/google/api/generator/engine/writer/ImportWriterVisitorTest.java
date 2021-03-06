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

import static junit.framework.Assert.assertEquals;

import com.google.api.generator.engine.ast.AssignmentExpr;
import com.google.api.generator.engine.ast.AstNode;
import com.google.api.generator.engine.ast.ClassDefinition;
import com.google.api.generator.engine.ast.MethodInvocationExpr;
import com.google.api.generator.engine.ast.Reference;
import com.google.api.generator.engine.ast.TypeNode;
import com.google.api.generator.engine.ast.Variable;
import com.google.api.generator.engine.ast.VariableExpr;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class ImportWriterVisitorTest {
  private static final String CURRENT_PACKAGE = "com.google.api.generator.engine.foobar";
  private ImportWriterVisitor writerVisitor;

  @Before
  public void setUp() {
    writerVisitor = new ImportWriterVisitor(CURRENT_PACKAGE);
  }

  @Test
  public void writeAssignmentExprImports() {
    Variable variable =
        Variable.builder().setName("clazz").setType(createType(AstNode.class)).build();
    VariableExpr variableExpr =
        VariableExpr.builder().setVariable(variable).setIsDecl(true).build();

    MethodInvocationExpr valueExpr =
        MethodInvocationExpr.builder()
            .setMethodName("createClass")
            .setStaticReferenceName(ClassDefinition.class.getSimpleName())
            .setReturnType(createType(ClassDefinition.class))
            .build();

    AssignmentExpr assignExpr =
        AssignmentExpr.builder().setVariableExpr(variableExpr).setValueExpr(valueExpr).build();

    assignExpr.accept(writerVisitor);
    assertEquals(
        writerVisitor.write(),
        String.format(
            createLines(2),
            "import com.google.api.generator.engine.ast.AstNode;\n",
            "import com.google.api.generator.engine.ast.ClassDefinition;\n\n"));
  }

  @Test
  public void writeAssignmentExprImports_staticAndNestedGenerics() {
    List<Reference> nestedSubGenerics =
        Arrays.asList(
            Reference.withClazz(ClassDefinition.class), Reference.withClazz(AstNode.class));
    Reference nestedGenericRef =
        Reference.builder().setClazz(Map.Entry.class).setGenerics(nestedSubGenerics).build();

    List<Reference> subGenerics =
        Arrays.asList(Reference.withClazz(AssignmentExpr.class), nestedGenericRef);
    Reference genericRef =
        Reference.builder().setClazz(Map.Entry.class).setGenerics(subGenerics).build();
    Reference reference =
        Reference.builder().setClazz(List.class).setGenerics(Arrays.asList(genericRef)).build();
    TypeNode type = TypeNode.withReference(reference);
    Variable variable = Variable.builder().setName("clazz").setType(type).build();
    VariableExpr variableExpr =
        VariableExpr.builder().setVariable(variable).setIsDecl(true).build();

    Reference returnReference =
        Reference.builder()
            .setClazz(ArrayList.class)
            .setGenerics(Arrays.asList(genericRef))
            .build();
    MethodInvocationExpr valueExpr =
        MethodInvocationExpr.builder()
            .setMethodName("doSomething")
            .setReturnType(TypeNode.withReference(returnReference))
            .build();

    AssignmentExpr assignExpr =
        AssignmentExpr.builder().setVariableExpr(variableExpr).setValueExpr(valueExpr).build();

    assignExpr.accept(writerVisitor);
    assertEquals(
        writerVisitor.write(),
        String.format(
            createLines(6),
            "import static java.util.Map.Entry;\n\n",
            "import com.google.api.generator.engine.ast.AssignmentExpr;\n",
            "import com.google.api.generator.engine.ast.AstNode;\n",
            "import com.google.api.generator.engine.ast.ClassDefinition;\n",
            "import java.util.ArrayList;\n",
            "import java.util.List;\n\n"));
  }

  private static TypeNode createType(Class clazz) {
    return TypeNode.withReference(Reference.withClazz(clazz));
  }

  private static String createLines(int numLines) {
    return new String(new char[numLines]).replace("\0", "%s");
  }
}
