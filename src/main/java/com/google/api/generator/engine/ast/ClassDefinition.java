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

package com.google.api.generator.engine.ast;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

@AutoValue
public abstract class ClassDefinition implements AstNode {
  // Required.
  public abstract ScopeNode scope();
  // Required.
  public abstract IdentifierNode classIdentifier();
  // Required for outer classes.
  @Nullable
  public abstract String packageString();

  public abstract boolean isNested();

  // Optional.
  // TODO(xiaozhenliu): Add a default-empty list of CommentStatements here.
  public abstract ImmutableSet<AnnotationNode> annotations();

  // Using a list helps with determinism in unit tests.
  public abstract ImmutableSet<TypeNode> implementsTypes();

  @Nullable
  public abstract TypeNode extendsType();

  public abstract boolean isStatic();

  public abstract boolean isFinal();

  public abstract boolean isAbstract();

  public abstract ImmutableList<Statement> statements();

  public abstract ImmutableList<MethodDefinition> methods();

  public abstract ImmutableList<ClassDefinition> nestedClasses();

  // Private.
  abstract String name();

  @Override
  public void accept(AstNodeVisitor visitor) {
    visitor.visit(this);
  }

  public static Builder builder() {
    return new AutoValue_ClassDefinition.Builder()
        .setIsNested(false)
        .setIsFinal(false)
        .setIsStatic(false)
        .setIsAbstract(false)
        .setAnnotations(ImmutableSet.of())
        .setImplementsTypes(ImmutableSet.of())
        .setStatements(ImmutableList.of())
        .setMethods(ImmutableList.of())
        .setNestedClasses(ImmutableList.of());
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setScope(ScopeNode scope);

    public abstract Builder setPackageString(String pkg);

    public abstract Builder setName(String name);

    public abstract Builder setIsNested(boolean isNested);

    public abstract Builder setAnnotations(Set<AnnotationNode> annotations);

    public abstract Builder setIsAbstract(boolean isAbstract);

    public abstract Builder setIsStatic(boolean isStatic);

    public abstract Builder setIsFinal(boolean isFinal);

    public abstract Builder setExtendsType(TypeNode type);

    public abstract Builder setImplementsTypes(Set<TypeNode> types);

    public abstract Builder setStatements(List<Statement> body);

    public abstract Builder setMethods(List<MethodDefinition> methods);

    public abstract Builder setNestedClasses(List<ClassDefinition> nestedClasses);

    // Private accessors.
    abstract String name();

    abstract ClassDefinition autoBuild();

    abstract Builder setClassIdentifier(IdentifierNode methodIdentifier);

    public ClassDefinition build() {
      IdentifierNode classIdentifier = IdentifierNode.builder().setName(name()).build();
      setClassIdentifier(classIdentifier);

      ClassDefinition classDef = autoBuild();

      // Only nested classes can forego having a package.
      if (!classDef.isNested()) {
        Preconditions.checkNotNull(
            classDef.packageString(), "Outer classes must have a package name defined");
        Preconditions.checkState(!classDef.isStatic(), "Outer classes cannot be static");
        Preconditions.checkState(
            !classDef.scope().equals(ScopeNode.PRIVATE), "Outer classes cannot be private");
      }

      // Abstract classes cannot be marked final.
      if (classDef.isAbstract()) {
        Preconditions.checkState(!classDef.isFinal(), "Abstract classes cannot be marked final");
      }

      // Check abstract extended type.
      if (classDef.extendsType() != null) {
        Preconditions.checkState(
            TypeNode.isReferenceType(classDef.extendsType()),
            "Classes cannot extend non-reference types");
        Preconditions.checkState(
            !classDef.implementsTypes().contains(classDef.extendsType()),
            "Classes cannot extend and implement the same type");
      }

      // Check implemented interface types.
      for (TypeNode implType : classDef.implementsTypes()) {
        Preconditions.checkState(
            TypeNode.isReferenceType(implType), "Classes cannot implement non-reference types");
      }

      for (Statement statement : classDef.statements()) {
        // TODO(xiaozhenliu): Add CommentStatement check here.
        Preconditions.checkState(
            statement instanceof ExprStatement,
            "Class statement type must be either an expression or comment statement");
        Expr expr = ((ExprStatement) statement).expression();
        if (expr instanceof VariableExpr) {
          VariableExpr variableExpr = (VariableExpr) expr;
          Preconditions.checkState(
              variableExpr.isDecl(), "Class expression variable statements must be declarations");
          Preconditions.checkState(
              !variableExpr.scope().equals(ScopeNode.LOCAL),
              "Class variable statement cannot have a local scope");
        } else {
          Preconditions.checkState(
              expr instanceof AssignmentExpr,
              "Class expression statement must be assignment or variable declaration");
          VariableExpr variableExpr = ((AssignmentExpr) expr).variableExpr();
          Preconditions.checkState(
              !variableExpr.scope().equals(ScopeNode.LOCAL),
              "Class variable in assignment statement cannot have a local scope");
        }
      }

      return classDef;
    }
  }
}
