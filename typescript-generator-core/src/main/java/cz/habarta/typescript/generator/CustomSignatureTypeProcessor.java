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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

        return findCustomSignatureFromAnnotationsIfPresent(annotations, member.getDeclaringClass(), member.toString())
                .map(typeScriptSignatureResult -> new Result(
                        new CustomSignatureType(typeScriptSignatureResult.signature()),
                        typeScriptSignatureResult.additionalClassesToProcess()))
                .orElse(null);
    }

    public static Optional<TypeScriptSignatureResult> findCustomSignatureFromAnnotationsIfPresent(
            Annotation[] annotations, Class<?> declaringClass, String ownerDescription) {
        return Arrays.stream(annotations)
                .map(annotation -> {
                    if (annotation instanceof TypeScriptSignature) {
                        return parseAnnotation((TypeScriptSignature) annotation);
                    } else if (annotation instanceof TypeScriptSignatureViaStaticMethod) {
                        return parseAnnotation((TypeScriptSignatureViaStaticMethod) annotation, declaringClass);
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .reduce((a, b) -> {
                    throw new RuntimeException(
                            "Only one signature annotation allowed, but found multiple on: " + ownerDescription);
                });
    }

    private static TypeScriptSignatureResult parseAnnotation(TypeScriptSignature annotation) {
        String signature = annotation.value();
        //noinspection ConstantConditions
        if (signature == null || signature.isEmpty()) {
            throw new RuntimeException("TypeScriptSignature value is required.");
        }

        return new TypeScriptSignatureResult() {
            @Override
            public String signature() {
                return signature;
            }

            @Override
            public List<Class<?>> additionalClassesToProcess() {
                return Arrays.asList(annotation.additionalClassesToProcess());
            }
        };
    }

    private static TypeScriptSignatureResult parseAnnotation(
            TypeScriptSignatureViaStaticMethod annotation, Class<?> declaringClass) {
        String staticMethodName = annotation.value();
        Class<?> clazz = annotation.declaringClass();
        if (clazz == void.class) {
            clazz = declaringClass;
        }
        try {
            Method method = clazz.getDeclaredMethod(staticMethodName);
            return (TypeScriptSignatureResult) method.invoke(null);
        } catch (Throwable t) {
            throw new RuntimeException(
                    "Unable to process TypeScriptSignatureViaStaticMethod, static method name: "
                            + staticMethodName + " class: " + clazz.getCanonicalName(), t);
        }
    }

}
