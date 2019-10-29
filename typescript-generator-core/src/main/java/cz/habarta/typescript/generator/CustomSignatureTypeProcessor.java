/*
 * Copyright (C) 2010-2019 Evergage, Inc.
 * All rights reserved.
 */

package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.emitter.TsCallableModel;
import cz.habarta.typescript.generator.emitter.TsPropertyModel;
import cz.habarta.typescript.generator.TsType.CustomSignatureType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * If {@link CustomSignature} annotation is present on element, returns {@link CustomSignatureType}.
 * - Java field will use {@link TsPropertyModel#getTsType()}
 * - Java method/constructor will use {@link TsCallableModel#getReturnType()}
 */
public class CustomSignatureTypeProcessor implements TypeProcessor {

    @Override
    public Result processType(Type javaType, Context context) {
        Member member = context.getMember();
        if (member == null && javaType instanceof Member) {
            member = (Member)javaType;
        }

        Annotation[] annotations;
        if (member instanceof Field) {
            annotations = ((Field) member).getDeclaredAnnotations();
        } else if (member instanceof Method) {
            annotations = ((Method) member).getDeclaredAnnotations();
        } else if (member instanceof Constructor) {
            annotations = ((Constructor) member).getDeclaredAnnotations();
        } else {
            return null;
        }

        return Arrays.stream(annotations)
                .filter(annotation -> annotation instanceof CustomSignature)
                .map(annotation -> {
                    CustomSignature customSignature = (CustomSignature) annotation;
                    String value = customSignature.value();
                    if (value.contains("::")) {
                        // Static method reference
                        String[] values = value.split("::");
                        if (values.length != 2) {
                            throw new RuntimeException("Unable to process CustomSignature value: " + value + "\nShould only contain :: once. ");
                        }
                        try {
                            Class<?> clazz = Class.forName(values[0]);
                            Method method = clazz.getDeclaredMethod(values[1]);
                            value = (String) method.invoke(null);
                        } catch (Throwable t) {
                            throw new RuntimeException("Unable to process CustomSignature value: " + value, t);
                        }
                    }
                    return new Result(new CustomSignatureType(value), customSignature.classes());
                })
                .findFirst()
                .orElse(null);
    }

}
