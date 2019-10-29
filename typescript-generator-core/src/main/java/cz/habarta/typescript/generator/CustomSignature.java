/*
 * Copyright (C) 2010-2019 Evergage, Inc.
 * All rights reserved.
 */

package cz.habarta.typescript.generator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Specify exact TypeScript signature to output verbatim for an element. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface CustomSignature {

    /** @return The custom TypeScript signature to output for this element.*/
    String value();

    /** @return Any classes the generator should processes as if they were 'discovered' in this element. */
    Class<?>[] classes() default {};

}
