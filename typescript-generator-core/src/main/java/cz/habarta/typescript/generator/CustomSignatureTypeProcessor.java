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
import java.util.Objects;

/**
 * Returns {@link CustomSignatureType} if supported annotations found on element:
 * {@link TypeScriptSignature} or {@link TypeScriptSignatureViaStaticMethod}
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

        Class<?> declaringClass = member.getDeclaringClass();
        return Arrays.stream(annotations)
                .map(annotation -> {
                    if (annotation instanceof TypeScriptSignature) {
                        TypeScriptSignature signatureAnnotation = (TypeScriptSignature) annotation;
                        String signature = signatureAnnotation.value();
                        //noinspection ConstantConditions
                        if (signature == null || signature.isEmpty()) {
                            throw new RuntimeException("TypeScriptSignature value is required.");
                        }
                        return new Result(new CustomSignatureType(signature), signatureAnnotation.additionalClassesToProcess());
                    } else if (annotation instanceof TypeScriptSignatureViaStaticMethod) {
                        TypeScriptSignatureViaStaticMethod signatureAnnotation = (TypeScriptSignatureViaStaticMethod) annotation;
                        String staticMethodName = signatureAnnotation.value();
                        Class<?> clazz = signatureAnnotation.declaringClass();
                        if (clazz == void.class) {
                            clazz = declaringClass;
                        }
                        try {
                            Method method = clazz.getDeclaredMethod(staticMethodName);
                            TypeScriptSignatureResult result = (TypeScriptSignatureResult) method.invoke(null);
                            return new Result(new CustomSignatureType(result.signature()), result.additionalClassesToProcess());
                        } catch (Throwable t) {
                            throw new RuntimeException(
                                    "Unable to process TypeScriptSignatureViaStaticMethod, static method name: "
                                            + staticMethodName + " class: " + clazz.getCanonicalName(), t);
                        }
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

}
