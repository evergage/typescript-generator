/*
 * Copyright (C) 2010-2019 Evergage, Inc.
 * All rights reserved.
 */

package cz.habarta.typescript.generator;

import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

public class CustomSignatureTest {

    @Test
    public void testCustomFieldSignatures() {
        String output = generateTypeScript(C1.class);
        // todo: jackson doesn't list static fields as props
        String expected = "\n" +
                "interface C1 {\n" +
                "    someStr: string;\n" +
                "    readonly someInt?: number;\n" +
                "    someSeeTwo: C2;\n" +
                "}\n" +
                "\n" +
                "interface C2 {\n" +
                "    answerOfLife?: number;\n" +
                "}\n";
        Assert.assertEquals(expected, output);
    }

    @Test
    public void testCustomEnumAsStringUnion() {
        String output = generateTypeScript(C3.class);
        String expected = "\n" +
                "interface C3 {\n" +
                "    e1: \"One\" | \"Two\" | \"Three\";\n" +
                "}\n";
        Assert.assertEquals(expected, output);
    }

    @Test
    public void testCustomGetterSetterSignatures() {
        String output = generateTypeScript(Bean1.class);
        String expected = "\n" +
                "interface Bean1 {\n" +
                "    someStr: string;\n" +
                "    readonly someInt?: number;\n" +
                "}\n";
        Assert.assertEquals(expected, output);
    }

    @Test
    public void testCustomAbstractMethods() {
        String output = generateTypeScript(Bean2.class);
        String expected = "\n" +
                "interface Bean2 {\n" +
                "\n" +
                "    fetchSomeString(one: string, two: number|undefined): string;\n" +
                "\n" +
                "    calculateSomeInt(one: string, two: number|undefined): number|undefined;\n" +
                "}\n";
        Assert.assertEquals(expected, output);
    }

    // todo: add support and test for default methods on interface

    public static class C1 {
        @CustomSignature("someStr: string")
        public String someString;

        @CustomSignature("readonly someInt?: number")
        public Integer someInteger;

        @CustomSignature("static readonly someNum: number")
        public static Number someNumber;

        @CustomSignature(value = "someSeeTwo: C2", classes = { C2.class })
        public C2 someC2;
    }

    public static class C2 {
        public static final String greeting = "hello";

        @Nullable
        public Integer answerOfLife;
    }

    public static class C3 {
        // Cannot work, value must be a compile-time constant
        //@CustomSignature(value = "e1: " + Arrays.stream(E1.values()).map(E1::name).collect(Collectors.joining("|")))

        // Can be accomplished via a static method reference
        @CustomSignature("cz.habarta.typescript.generator.CustomSignatureTest$C3::e1CustomSignature")
        public String e1;

        @SuppressWarnings("unused - used by CustomSignature annotation")
        public static String e1CustomSignature() {
            return "e1: " + Arrays.stream(E1.values()).map(e -> '"' + e.name() + '"').collect(Collectors.joining(" | "));
        }
    }

    public enum E1 {
        One,
        Two,
        Three
    }

    public static class Bean1 {
        private String string;
        private Integer integer;

        @CustomSignature("someStr: string")
        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }

        // ModelCompiler doesn't currently detect lack of setter, but we can add readonly easily via CustomSignature
        @CustomSignature("readonly someInt?: number")
        @Nullable
        public Integer getInteger() {
            return integer;
        }
    }

    // ModelParser doesn't currently detect param names and optional annotations, and ignores
    // optional annotations on return type, but we can overcome these via CustomSignature
    public static abstract class Bean2 {
        @CustomSignature("fetchSomeString(one: string, two: number|undefined): string")
        public abstract String fetchSomeString(String one, @Nullable Integer two);

        @CustomSignature("calculateSomeInt(one: string, two: number|undefined): number|undefined")
        @Nullable
        public abstract Integer calculateSomeInt(String one, @Nullable Integer two);
    }

    private String generateTypeScript(Class<?> clazz) {
        final Settings settings = TestUtils.settings();
        //settings.emitSAMs = EmitSAMStrictness.byAnnotationOnly;
        settings.emitAbstractMethodsInBeans = true;
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.mapClasses = ClassMapping.asInterfaces;
        settings.optionalAnnotations = Collections.singletonList(Nullable.class);
        return new TypeScriptGenerator(settings).generateTypeScript(Input.from(clazz));
    }
}
