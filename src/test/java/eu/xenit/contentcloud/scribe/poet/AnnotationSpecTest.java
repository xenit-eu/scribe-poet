/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.xenit.contentcloud.scribe.poet;

import com.google.testing.compile.CompilationRule;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.function.Function;

import javax.lang.model.element.TypeElement;
import org.junit.Rule;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

public final class AnnotationSpecTest {

  @Retention(RetentionPolicy.RUNTIME)
  public @interface AnnotationA {
  }

  @Inherited
  @Retention(RetentionPolicy.RUNTIME)
  public @interface AnnotationB {
  }

  @Retention(RetentionPolicy.RUNTIME)
  public @interface AnnotationC {
    String value();
  }

  public enum Breakfast {
    WAFFLES, PANCAKES;
    public String toString() { return name() + " with cherries!"; };
  }

  @Retention(RetentionPolicy.RUNTIME)
  public @interface HasDefaultsAnnotation {

    byte a() default 5;

    short b() default 6;

    int c() default 7;

    long d() default 8;

    float e() default 9.0f;

    double f() default 10.0;

    char[] g() default {0, 0xCAFE, 'z', '€', 'ℕ', '"', '\'', '\t', '\n'};

    boolean h() default true;

    Breakfast i() default Breakfast.WAFFLES;

    AnnotationA j() default @AnnotationA();

    String k() default "maple";

    Class<? extends Annotation> l() default AnnotationB.class;

    int[] m() default {1, 2, 3};

    Breakfast[] n() default {Breakfast.WAFFLES, Breakfast.PANCAKES};

    Breakfast o();

    int p();

    AnnotationC q() default @AnnotationC("foo");

    Class<? extends Number>[] r() default {Byte.class, Short.class, Integer.class, Long.class};

  }

  @HasDefaultsAnnotation(
      o = Breakfast.PANCAKES,
      p = 1701,
      f = 11.1,
      m = {9, 8, 1},
      l = Override.class,
      j = @AnnotationA,
      q = @AnnotationC("bar"),
      r = {Float.class, Double.class})
  public class IsAnnotated {
    // empty
  }

  @Rule public final CompilationRule compilation = new CompilationRule();

  @Test public void equalsAndHashCode() {
    AnnotationSpec a = AnnotationSpec.builder(AnnotationC.class).build();
    AnnotationSpec b = AnnotationSpec.builder(AnnotationC.class).build();
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    a = AnnotationSpec.builder(AnnotationC.class).addMember("value", "$S", "123").build();
    b = AnnotationSpec.builder(AnnotationC.class).addMember("value", "$S", "123").build();
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
  }

  @Test public void defaultAnnotation() {
    String name = IsAnnotated.class.getCanonicalName();
    TypeElement element = compilation.getElements().getTypeElement(name);
    AnnotationSpec annotation = AnnotationSpec.get(element.getAnnotationMirrors().get(0));

    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addAnnotation(annotation)
        .build();

    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import eu.xenit.contentcloud.scribe.poet.AnnotationSpecTest;\n"
        + "import java.lang.Double;\n"
        + "import java.lang.Float;\n"
        + "import java.lang.Override;\n"
        + "\n"
        + "@AnnotationSpecTest.HasDefaultsAnnotation("
            + "o = AnnotationSpecTest.Breakfast.PANCAKES, p = 1701, f = 11.1, m = {9, 8, 1}, "
            + "l = Override.class, j = @AnnotationSpecTest.AnnotationA, q = @AnnotationSpecTest.AnnotationC(\"bar\"), "
            + "r = {Float.class, Double.class})\n"
        + "class Taco {\n"
        + "}\n");
  }

  @Test public void defaultAnnotationWithImport() {
    String name = IsAnnotated.class.getCanonicalName();
    TypeElement element = compilation.getElements().getTypeElement(name);
    AnnotationSpec annotation = AnnotationSpec.get(element.getAnnotationMirrors().get(0));
    TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(IsAnnotated.class.getSimpleName());
    typeBuilder.addAnnotation(annotation);
    JavaFile file = JavaFile.builder("eu.xenit.contentcloud.scribe.poet", typeBuilder.build()).build();
    assertThat(file.toString()).isEqualTo(
        "package eu.xenit.contentcloud.scribe.poet;\n"
            + "\n"
            + "import java.lang.Double;\n"
            + "import java.lang.Float;\n"
            + "import java.lang.Override;\n"
            + "\n"
                + "@AnnotationSpecTest.HasDefaultsAnnotation("
                + "o = AnnotationSpecTest.Breakfast.PANCAKES, p = 1701, f = 11.1, m = {9, 8, 1}, "
                + "l = Override.class, j = @AnnotationSpecTest.AnnotationA, q = @AnnotationSpecTest.AnnotationC(\"bar\"), "
                + "r = {Float.class, Double.class})\n"
            + "class IsAnnotated {\n"
            + "}\n"
    );
  }

  @Test public void emptyArray() {
    AnnotationSpec.Builder builder = AnnotationSpec.builder(HasDefaultsAnnotation.class);
    builder.addMember("n", "$L", "{}");
    assertThat(builder.build().toString()).isEqualTo(
        "@eu.xenit.contentcloud.scribe.poet.AnnotationSpecTest.HasDefaultsAnnotation(" + "n = {}" + ")");
    builder.addMember("m", "$L", "{}");
    assertThat(builder.build().toString())
        .isEqualTo(
            "@eu.xenit.contentcloud.scribe.poet.AnnotationSpecTest.HasDefaultsAnnotation("
                + "n = {}, m = {}"
                + ")");
  }

  @Test public void dynamicArrayOfEnumConstants() {
    AnnotationSpec.Builder builder = AnnotationSpec.builder(HasDefaultsAnnotation.class);
    builder.addMember("n", "$T.$L", Breakfast.class, Breakfast.PANCAKES.name());
    assertThat(builder.build().toString()).isEqualTo(
        "@eu.xenit.contentcloud.scribe.poet.AnnotationSpecTest.HasDefaultsAnnotation("
            + "n = eu.xenit.contentcloud.scribe.poet.AnnotationSpecTest.Breakfast.PANCAKES"
            + ")");

    // builder = AnnotationSpec.builder(HasDefaultsAnnotation.class);
    builder.addMember("n", "$T.$L", Breakfast.class, Breakfast.WAFFLES.name());
    builder.addMember("n", "$T.$L", Breakfast.class, Breakfast.PANCAKES.name());
    assertThat(builder.build().toString()).isEqualTo(
        "@eu.xenit.contentcloud.scribe.poet.AnnotationSpecTest.HasDefaultsAnnotation("
            + "n = {"
            + "eu.xenit.contentcloud.scribe.poet.AnnotationSpecTest.Breakfast.PANCAKES"
            + ", eu.xenit.contentcloud.scribe.poet.AnnotationSpecTest.Breakfast.WAFFLES"
            + ", eu.xenit.contentcloud.scribe.poet.AnnotationSpecTest.Breakfast.PANCAKES"
            + "})");

    builder = builder.build().toBuilder(); // idempotent
    assertThat(builder.build().toString()).isEqualTo(
        "@eu.xenit.contentcloud.scribe.poet.AnnotationSpecTest.HasDefaultsAnnotation("
            + "n = {"
            + "eu.xenit.contentcloud.scribe.poet.AnnotationSpecTest.Breakfast.PANCAKES"
            + ", eu.xenit.contentcloud.scribe.poet.AnnotationSpecTest.Breakfast.WAFFLES"
            + ", eu.xenit.contentcloud.scribe.poet.AnnotationSpecTest.Breakfast.PANCAKES"
            + "})");

    builder.addMember("n", "$T.$L", Breakfast.class, Breakfast.WAFFLES.name());
    assertThat(builder.build().toString()).isEqualTo(
        "@eu.xenit.contentcloud.scribe.poet.AnnotationSpecTest.HasDefaultsAnnotation("
            + "n = {"
            + "eu.xenit.contentcloud.scribe.poet.AnnotationSpecTest.Breakfast.PANCAKES"
            + ", eu.xenit.contentcloud.scribe.poet.AnnotationSpecTest.Breakfast.WAFFLES"
            + ", eu.xenit.contentcloud.scribe.poet.AnnotationSpecTest.Breakfast.PANCAKES"
            + ", eu.xenit.contentcloud.scribe.poet.AnnotationSpecTest.Breakfast.WAFFLES"
            + "})");
  }

  @Test public void defaultAnnotationToBuilder() {
    String name = IsAnnotated.class.getCanonicalName();
    TypeElement element = compilation.getElements().getTypeElement(name);
    AnnotationSpec.Builder builder = AnnotationSpec.get(element.getAnnotationMirrors().get(0))
        .toBuilder();
    builder.addMember("m", "$L", 123);
    assertThat(builder.build().toString()).isEqualTo(
        "@eu.xenit.contentcloud.scribe.poet.AnnotationSpecTest.HasDefaultsAnnotation("
            + "o = eu.xenit.contentcloud.scribe.poet.AnnotationSpecTest.Breakfast.PANCAKES"
            + ", p = 1701"
            + ", f = 11.1"
            + ", m = {9, 8, 1, 123}"
            + ", l = java.lang.Override.class"
            + ", j = @eu.xenit.contentcloud.scribe.poet.AnnotationSpecTest.AnnotationA"
            + ", q = @eu.xenit.contentcloud.scribe.poet.AnnotationSpecTest.AnnotationC(\"bar\")"
            + ", r = {java.lang.Float.class, java.lang.Double.class}"
            + ")");
  }

  @Test public void reflectAnnotation() {
    HasDefaultsAnnotation annotation = IsAnnotated.class.getAnnotation(HasDefaultsAnnotation.class);
    AnnotationSpec spec = AnnotationSpec.get(annotation);
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addAnnotation(spec)
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import eu.xenit.contentcloud.scribe.poet.AnnotationSpecTest;\n"
        + "import java.lang.Double;\n"
        + "import java.lang.Float;\n"
        + "import java.lang.Override;\n"
        + "\n"
        + "@AnnotationSpecTest.HasDefaultsAnnotation("
        + "f = 11.1, l = Override.class, m = {9, 8, 1}, o = AnnotationSpecTest.Breakfast.PANCAKES, "
        + "p = 1701, q = @AnnotationSpecTest.AnnotationC(\"bar\"), r = {Float.class, Double.class})\n"
        + "class Taco {\n"
        + "}\n");
  }

  @Test public void reflectAnnotationWithDefaults() {
    HasDefaultsAnnotation annotation = IsAnnotated.class.getAnnotation(HasDefaultsAnnotation.class);
    AnnotationSpec spec = AnnotationSpec.get(annotation, true);
    TypeSpec taco = TypeSpec.classBuilder("Taco")
        .addAnnotation(spec)
        .build();
    assertThat(toString(taco)).isEqualTo(""
        + "package com.squareup.tacos;\n"
        + "\n"
        + "import eu.xenit.contentcloud.scribe.poet.AnnotationSpecTest;\n"
        + "import java.lang.Double;\n"
        + "import java.lang.Float;\n"
        + "import java.lang.Override;\n"
        + "\n"
        + "@AnnotationSpecTest.HasDefaultsAnnotation("
        + "a = 5, "
        + "b = 6, "
        + "c = 7, "
        + "d = 8, "
        + "e = 9.0f, "
        + "f = 11.1, "
        + "g = {'\\u0000', '쫾', 'z', '€', 'ℕ', '\"', '\\'', '\\t', '\\n'}, "
        + "h = true, "
        + "i = AnnotationSpecTest.Breakfast.WAFFLES, "
        + "j = @AnnotationSpecTest.AnnotationA, "
        + "k = \"maple\", "
        + "l = Override.class, "
        + "m = {9, 8, 1}, "
        + "n = {AnnotationSpecTest.Breakfast.WAFFLES, AnnotationSpecTest.Breakfast.PANCAKES}, "
        + "o = AnnotationSpecTest.Breakfast.PANCAKES, "
        + "p = 1701, "
        + "q = @AnnotationSpecTest.AnnotationC(\"bar\"), "
        + "r = {Float.class, Double.class}"
        + ")\n"
        + "class Taco {\n"
        + "}\n");

    System.out.println(""
            + "package com.squareup.tacos;\n"
            + "\n"
            + "import com.squareup.javapoet.AnnotationSpecTest;\n"
            + "import java.lang.Double;\n"
            + "import java.lang.Float;\n"
            + "import java.lang.Override;\n"
            + "\n"
            + "@AnnotationSpecTest.HasDefaultsAnnotation("
            + "a = 5, "
            + "b = 6, "
            + "c = 7, "
            + "d = 8, "
            + "e = 9.0f, "
            + "f = 11.1, "
            + "g = {'\\u0000', '쫾', 'z', '€', 'ℕ', '\"', '\\'', '\\t', '\\n'}, "
            + "h = true, "
            + "i = AnnotationSpecTest.Breakfast.WAFFLES, "
            + "j = @AnnotationSpecTest.AnnotationA, "
            + "k = \"maple\", "
            + "l = Override.class, "
            + "m = {9, 8, 1}, "
            + "n = {AnnotationSpecTest.Breakfast.WAFFLES, AnnotationSpecTest.Breakfast.PANCAKES}, "
            + "o = AnnotationSpecTest.Breakfast.PANCAKES, "
            + "p = 1701, "
            + "q = @AnnotationSpecTest.AnnotationC(\"bar\"), "
            + "r = {Float.class, Double.class}"
            + ")\n"
            + "class Taco {\n"
            + "}\n");
  }

  @Test public void disallowsNullMemberName() {
    AnnotationSpec.Builder builder = AnnotationSpec.builder(HasDefaultsAnnotation.class);
    try {
      AnnotationSpec.Builder $L = builder.addMember(null, "$L", "");
      fail($L.build().toString());
    } catch (NullPointerException e) {
      assertThat(e).hasMessageThat().isEqualTo("name == null");
    }
  }

  @Test public void requiresValidMemberName() {
    AnnotationSpec.Builder builder = AnnotationSpec.builder(HasDefaultsAnnotation.class);
    try {
      AnnotationSpec.Builder $L = builder.addMember("@", "$L", "");
      fail($L.build().toString());
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat().isEqualTo("not a valid name: @");
    }
  }

  @Test public void modifyMembers() {
    AnnotationSpec.Builder builder = AnnotationSpec.builder(SuppressWarnings.class)
            .addMember("value", "$S", "Foo");
    
    builder.members.clear();
    builder.members.put("value", Arrays.asList(CodeBlock.of("$S", "Bar")));

    assertThat(builder.build().toString()).isEqualTo("@java.lang.SuppressWarnings(\"Bar\")");
  }

  private String toString(TypeSpec typeSpec) {
    return JavaFile.builder("com.squareup.tacos", typeSpec).build().toString();
  }
}
