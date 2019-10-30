/*
 * Copyright (C) 2010-2019 Evergage, Inc.
 * All rights reserved.
 */

package cz.habarta.typescript.generator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides a string reference to a static method that can dynamically return the
 * TypeScript signature to output verbatim for an element.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface TypeScriptSignatureViaStaticMethod {

    /**
     * @return Name of the static method that will dynamically provide the TypeScript signature to use.
     *         For example:
     *         <pre>{@code
     *         package com.example;
     *
     *         public class MyClass {
     *             @TypeScriptSignatureViaStaticMethod("myEnumCustomSignature")
     *             public String myEnum;
     *
     *             public static TypeScriptSignatureResult myEnumCustomSignature() {
     *                 return () -> "myEnum: " + Arrays.stream(MyEnum.values()).map(e -> '"' + e.name() + '"').collect(Collectors.joining(" | "));
     *             }
     *         }
     *
     *         public enum MyEnum {
     *             One,
     *             Two,
     *             Three
     *         }
     *         }</pre>
     */
    String value();

    /**
     * The class declaring the static method name provided by {@link #value()}.
     * Defaults to using the class containing the element using this annotation (via marker void.class).
     */
    Class<?> declaringClass() default void.class;

}
