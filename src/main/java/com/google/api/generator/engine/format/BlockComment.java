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

package com.google.api.generator.engine.format;

import com.google.auto.value.AutoValue;
import com.google.googlejavaformat.java.Formatter;

@AutoValue
public abstract class BlockComment implements Comment {
  public abstract String comment();

  public static Builder builder() {
    return new AutoValue_BlockComment.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setComment(String comment);

    public abstract BlockComment build();
  }

  @Override
  public String write() {
    // split the comments by new line and embrace `/** */` with the comment block.
    String sourceString = comment();
    String formattedSource = "";
    try {
      formattedSource = new Formatter().formatSource("/** " + sourceString + " */");
      return formattedSource;
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
    return formattedSource;
  }
}