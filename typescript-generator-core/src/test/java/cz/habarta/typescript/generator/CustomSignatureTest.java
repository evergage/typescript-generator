/*
 * Copyright (C) 2010-2019 Evergage, Inc.
 * All rights reserved.
 */

package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nullable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class CustomSignatureTest {

    private TypeScriptGenerator typeScriptGenerator;

    @Before
    public void initialize() {
        Settings settings = defaultSettings();
        typeScriptGenerator = new TypeScriptGenerator(settings);
    }

    private Settings defaultSettings() {
        Settings settings = TestUtils.settings();
        //settings.emitSAMs = EmitSAMStrictness.byAnnotationOnly;
        settings.emitAbstractMethodsInBeans = true;
        settings.emitDefaultMethods = true;
        settings.emitStaticMethods = true;
        settings.emitOtherMethods = true;
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.mapClasses = ClassMapping.asInterfaces;
        settings.optionalAnnotations = Collections.singletonList(Nullable.class);
        settings.sortDeclarations = true;
        settings.sortTypeDeclarations = true;
        settings.excludePropertyAnnotations = Collections.singletonList(ApiExclude.class);
        return settings;
    }

    public static class TestClassBasic {
        @TypeScriptSignature("someStr: string")
        public String someString;

        @TypeScriptSignature("readonly someInt?: number")
        public Integer someInteger;

        @TypeScriptSignature("static readonly someNum: number")
        public static Number someNumber;

        @TypeScriptSignature(value = "someTest: TestClassProvidedViaAnnotation", additionalClassesToProcess = { TestClassProvidedViaAnnotation.class })
        public TestClassProvidedViaAnnotation test;
    }

    public static class TestClassProvidedViaAnnotation {
        public static final String greeting = "hello";

        @Nullable
        public Integer answerOfLife;
    }

    @Test
    public void testCustomFieldSignatures() {
        // todo: jackson doesn't list static fields as props
        assertExpectedOutputforClass(TestClassBasic.class,"\n" +
                "interface TestClassBasic {\n" +
                "    readonly someInt?: number;\n" +
                "    someStr: string;\n" +
                "    someTest: TestClassProvidedViaAnnotation;\n" +
                "}\n" +
                "\n" +
                "interface TestClassProvidedViaAnnotation {\n" +
                "    answerOfLife?: number;\n" +
                "}\n");
    }

    public static class TestClassWithSignatureViaStaticMethod {
        // Cannot work, value must be a compile-time constant
        //@TypeScriptSignature(value = "e1: " + Arrays.stream(E1.values()).map(E1::name).collect(Collectors.joining("|")))

        // Can be accomplished via a static method reference
        @TypeScriptSignatureViaStaticMethod("testCustomSignature")
        public String test;

        @ApiExclude
        public static TypeScriptSignatureResult testCustomSignature() {
            return () ->
                    "test: " + Arrays.stream(TestEnum.values()).map(e -> '"' + e.name() + '"').collect(Collectors.joining(" | "));
        }
    }

    public enum TestEnum {
        One,
        Two,
        Three
    }

    @Test
    public void testCustomEnumAsStringUnion() {
        assertExpectedOutputforClass(TestClassWithSignatureViaStaticMethod.class, "\n" +
                "interface TestClassWithSignatureViaStaticMethod {\n" +
                "    test: \"One\" | \"Two\" | \"Three\";\n" +
                "}\n");
    }

    public static class TestBean {
        private String string;
        private Integer integer;

        @TypeScriptSignature("someStr: string")
        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }

        // ModelCompiler doesn't currently detect lack of setter, but we can add readonly easily via annotation
        @TypeScriptSignature("readonly someInt?: number")
        @Nullable
        public Integer getInteger() {
            return integer;
        }
    }

    @Test
    public void testCustomBeanProperties() {
        assertExpectedOutputforClass(TestBean.class, "\n" +
                "interface TestBean {\n" +
                "    readonly someInt?: number;\n" +
                "    someStr: string;\n" +
                "}\n");
    }

    public static class TestBeanAvoidProps {
        private String string;
        private Integer integer;

        @JsonIgnore
        public String getString() {
            return string;
        }

        @TypeScriptSignature("setString(str: string): void;")
        public void setString(String string) {
            this.string = string;
        }

        @JsonIgnore
        public Integer getInteger() {
            return integer;
        }
    }

    @Test
    public void testBeanKeepGettersSettersAsMethods() {
        assertExpectedOutputforClass(TestBeanAvoidProps.class, "\n" +
                "interface TestBeanAvoidProps {\n" +
                "\n" +
                "    getInteger(): number;\n" +
                "\n" +
                "    getString(): string;\n" +
                "\n" +
                "    setString(str: string): void;;\n" +
                "}\n");
    }

    // ModelParser doesn't currently detect param names and optional annotations, and ignores
    // optional annotations on return type, but we can overcome these via annotation
    public static abstract class TestClassAbstract {
        @TypeScriptSignature("fetchSomeString(one: string, two: number|undefined): string")
        public abstract String fetchSomeString(String one, @Nullable Integer two);

        @TypeScriptSignature("calculateSomeInt(one: string, two: number|undefined): number|undefined")
        @Nullable
        public abstract Integer calculateSomeInt(String one, @Nullable Integer two);
    }

    @Test
    public void testCustomAbstractMethods() {
        assertExpectedOutputforClass(TestClassAbstract.class, "\n" +
                "interface TestClassAbstract {\n" +
                "\n" +
                "    calculateSomeInt(one: string, two: number|undefined): number|undefined;\n" +
                "\n" +
                "    fetchSomeString(one: string, two: number|undefined): string;\n" +
                "}\n");
    }

    public static class TestClassWithConstructorsAndStaticMethods {
        private String string = null;
        private Integer integer = null;

        public TestClassWithConstructorsAndStaticMethods(String string) {
            this.string = string;
        }

        public TestClassWithConstructorsAndStaticMethods(Integer integer) {
            this.integer = integer;
        }

        public TestClassWithConstructorsAndStaticMethods(String string, Integer integer) {
            this.string = string;
            this.integer = integer;
        }

        public static String fetchSomeString(String one, Integer two) {
            return "";
        }

        @TypeScriptSignature("static calculateSomeInt(one: string, two: number | null): number | null")
        @Nullable
        public static Integer calculateSomeInt(String one, @Nullable Integer two) {
            return null;
        }
    }

    // Constructors are not supported yet (interfaces could use 'new', classes could use 'constructor')
    @Test
    public void testConstructorsAndStaticMethods() {
        assertExpectedOutputforClass(TestClassWithConstructorsAndStaticMethods.class, "\n" +
                "interface TestClassWithConstructorsAndStaticMethods {\n" +
                "\n" +
                "    static calculateSomeInt(one: string, two: number | null): number | null;\n" +
                "\n" +
                "    static fetchSomeString(arg0: string, arg1: number): string;\n" +
                "}\n");
    }

    public interface TestInterface {
        String fetchSomeString(String one, Integer two);

        // ModelParser doesn't currently detect optional return value, param name/nullable, so using annotation
        @TypeScriptSignature("calculateSomeInt(one: string, two: number | null): number | null")
        @Nullable
        Integer calculateSomeInt(String one, @Nullable Integer two);

        // ModelParser doesn't currently detect optional method via default, so using annotation
        @TypeScriptSignature("doStuffForString?(one: string, two: number): string")
        default String doStuffForString(String one, Integer two) { return ""; }

        @TypeScriptSignature("doStuffForInteger?(one: string, two: number | null): number | null")
        @Nullable
        default Integer doStuffForInteger(String one, @Nullable Integer two) { return null; }
    }

    @Test
    public void testInterface() {
        assertExpectedOutputforClass(TestInterface.class, "\n" +
                "interface TestInterface {\n" +
                "\n" +
                "    calculateSomeInt(one: string, two: number | null): number | null;\n" +
                "\n" +
                "    doStuffForInteger?(one: string, two: number | null): number | null;\n" +
                "\n" +
                "    doStuffForString?(one: string, two: number): string;\n" +
                "\n" +
                "    fetchSomeString(arg0: string, arg1: number): string;\n" +
                "}\n");
    }

    public interface TestInterfaceWithOptionalParams {
        @TypeScriptSignature("doStuff(str: string, integer?: number, bool?: boolean): void")
        void doStuff(String string);

        @ApiExclude
        void doStuff(String string, Integer integer);

        @ApiExclude
        void doStuff(String string, Integer integer, Boolean bool);
    }

    @Test
    public void testInterfaceMethodBuildingUpOptionalParams() {
        assertExpectedOutputforClass(TestInterfaceWithOptionalParams.class, "\n" +
                "interface TestInterfaceWithOptionalParams {\n" +
                "\n" +
                "    doStuff(str: string, integer?: number, bool?: boolean): void;\n" +
                "}\n");
    }

    @Implementable
    public interface TestImplementable {
        String fetchSomeString(String one, Integer two);

        @TypeScriptSignature("doStuffForInteger?(one: string, two: number | null): number | null")
        @Nullable
        default Integer doStuffForInteger(String one, @Nullable Integer two) { return null; }

        @ApiExclude
        void forSomeReasonDontExpose();
    }

    @Test
    public void testImplementableInterface() {
        Settings settings = defaultSettings();
        settings.includePropertyAnnotations = Arrays.asList(Implementable.class, Export.class);
        typeScriptGenerator = new TypeScriptGenerator(settings);

        assertExpectedOutputforClass(TestImplementable.class, "\n" +
                "interface TestImplementable {\n" +
                "\n" +
                "    doStuffForInteger?(one: string, two: number | null): number | null;\n" +
                "\n" +
                "    fetchSomeString(arg0: string, arg1: number): string;\n" +
                "}\n");
    }

    public class TestClassWithExports {
        public String fieldNotExported;

        @TypeScriptSignature("someStr: string")
        @Export
        public String someString;

        public void methodNotExported() {}

        @TypeScriptSignature("doStuffForInteger?(one: string, two: number | null): number | null")
        @Export
        @Nullable
        Integer doStuffForInteger(String one, @Nullable Integer two) { return null; }
    }

    @Test
    public void testExport() {
        Settings settings = defaultSettings();
        settings.includePropertyAnnotations = Arrays.asList(Implementable.class, Export.class);
        typeScriptGenerator = new TypeScriptGenerator(settings);

        assertExpectedOutputforClass(TestClassWithExports.class, "\n" +
                "interface TestClassWithExports {\n" +
                "    someStr: string;\n" +
                "\n" +
                "    doStuffForInteger?(one: string, two: number | null): number | null;\n" +
                "}\n");
    }

    private void assertExpectedOutputforClass(Class<?> clazz, String expectedOutput) {
        String output = typeScriptGenerator.generateTypeScript(Input.from(clazz));
        Assert.assertEquals(expectedOutput, output);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface ApiExclude {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface Implementable {
    }

    @Target({ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Export {
    }
}
