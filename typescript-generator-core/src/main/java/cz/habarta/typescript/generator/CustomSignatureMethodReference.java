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
public @interface CustomSignatureMethodReference {

    /**
     * @return String reference to a static method that will dynamically provide the TypeScript signature to use.
     *         Should be in same class as this element, to easily guarantee the class will be found.
     *         For example:
     *         <pre>{@code
     *         package com.example;
     *
     *         public class MyClass {
     *             @CustomSignatureMethodReference("com.example.MyClass::myEnumCustomSignature")
     *             public String myEnum;
     *
     *             public static String myEnumCustomSignature() {
     *                 return "myEnum: " + Arrays.stream(MyEnum.values()).map(e -> '"' + e.name() + '"').collect(Collectors.joining(" | "));
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

    /** @return Any classes the generator should processes as if they were 'discovered' in this element. */
    Class<?>[] classes() default {};

}
