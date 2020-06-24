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
public abstract class MethodDefinition implements AstNode {
  // Required.
  public abstract ScopeNode scope();
  // Required.
  public abstract TypeNode returnType();
  // Required.
  public abstract IdentifierNode methodIdentifier();

  // TODO(xiahzhenliu): Add a Nullable JavaDoc here.
  public abstract ImmutableSet<AnnotationNode> annotations();

  // Using a list helps with determinism in unit tests.
  public abstract ImmutableSet<TypeNode> throwsExceptions();

  public abstract ImmutableList<VariableExpr> arguments();

  public abstract boolean isStatic();

  public abstract boolean isFinal();

  public abstract boolean isAbstract();

  public abstract ImmutableList<Statement> body();

  @Nullable
  public abstract Expr returnExpr();

  abstract boolean isOverride();

  abstract String name();

  @Override
  public void accept(AstNodeVisitor visitor) {
    visitor.visit(this);
  }

  public static Builder builder() {
    return new AutoValue_MethodDefinition.Builder()
        .setArguments(ImmutableList.of())
        .setIsAbstract(false)
        .setIsFinal(false)
        .setIsStatic(false)
        .setAnnotations(ImmutableSet.of())
        .setThrowsExceptions(ImmutableSet.of())
        .setBody(ImmutableList.of())
        .setIsOverride(false);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setScope(ScopeNode scope);

    public abstract Builder setReturnType(TypeNode type);

    public abstract Builder setName(String name);

    public Builder setAnnotations(Set<AnnotationNode> annotations) {
      annotationsBuilder().addAll(annotations);
      return this;
    }

    public abstract Builder setIsStatic(boolean isStatic);

    public abstract Builder setIsFinal(boolean isFinal);

    public abstract Builder setIsAbstract(boolean isAbstract);

    public abstract Builder setThrowsExceptions(Set<TypeNode> exceptionTypes);

    public abstract Builder setArguments(List<VariableExpr> arguments);

    public abstract Builder setBody(List<Statement> body);

    public abstract Builder setReturnExpr(Expr returnExpr);

    public abstract Builder setIsOverride(boolean isOverride);

    // Private accessors.

    abstract ImmutableSet.Builder<AnnotationNode> annotationsBuilder();

    abstract String name();

    abstract boolean isOverride();

    abstract boolean isAbstract();

    abstract boolean isFinal();

    abstract boolean isStatic();

    abstract ScopeNode scope();

    abstract MethodDefinition autoBuild();

    abstract Builder setMethodIdentifier(IdentifierNode methodIdentifier);

    public MethodDefinition build() {
      IdentifierNode methodIdentifier = IdentifierNode.builder().setName(name()).build();
      setMethodIdentifier(methodIdentifier);

      // Abstract and modifier checking.
      if (isAbstract()) {
        Preconditions.checkState(
            !isFinal() && !isStatic() && !scope().equals(ScopeNode.PRIVATE),
            "Abstract mehtods cannot be static, final, or private");
      }

      // If this method overrides another, ensure that the Override annotaiton is the last one.
      if (isOverride()) {
        annotationsBuilder().add(AnnotationNode.OVERRIDE);
      }

      MethodDefinition method = autoBuild();

      if (!method.returnType().equals(TypeNode.VOID)) {
        Preconditions.checkState(
            method.returnExpr() != null,
            "Method with non-void return type must have a return expression");
      }

      // Type-checking.
      if (method.returnExpr() != null) {
        Preconditions.checkState(
            method.returnType().equals(method.returnExpr().type()),
            "Method return type does not match the return expression type");
      }

      for (VariableExpr varExpr : method.arguments()) {
        Preconditions.checkState(
            varExpr.isDecl(),
            String.format(
                "Argument %s must be a variable declaration", varExpr.variable().identifier()));
      }

      for (TypeNode exceptionType : method.throwsExceptions()) {
        Preconditions.checkState(
            TypeNode.isExceptionType(exceptionType),
            String.format("Type %s is not an exception type", exceptionType.reference()));
        Preconditions.checkState(
            !RuntimeException.class.isAssignableFrom(exceptionType.reference().clazz()),
            String.format(
                "RuntimeException type %s does not need to be thrown",
                exceptionType.reference().name()));
      }

      return method;
    }
  }
}
