/*
 * Copyright (C) 2010-2019 Evergage, Inc.
 * All rights reserved.
 */

package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.util.Pair;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import static java.util.stream.Collectors.toMap;
import java.util.stream.Stream;

public class SAMTypeProcessor implements TypeProcessor {

    private EmitSAMStrictness emitSAMs;

    public SAMTypeProcessor(EmitSAMStrictness emitSAMs) {
        this.emitSAMs = emitSAMs;
    }

    @Override
    public Result processType(Type javaType, Context context) {

        Stream<Pair<Predicate<Type>, SAMProcessor>> processors = Stream.of(
                Pair.of(this::shouldProcessParameterizedType, this::processParameterizedSAM),
                Pair.of(this::shouldProcessNonParameterizedType, this::processNonParameterizedSAM));

        Function<Pair<Predicate<Type>, SAMProcessor>, Optional<Result>> processorRunner = processor -> {
            if (processor.getValue1().test(javaType)) {
                return extractAndProcessSAM(javaType, context, processor.getValue2());
            }
            return Optional.empty();
        };

        return processors
                .map(processorRunner)
                .filter(Optional::isPresent)
                .findFirst()
                .orElse(Optional.empty())
                .orElse(null);
    }

    private Optional<Result> extractAndProcessSAM(Type javaType, Context ctx, SAMProcessor samProcessor) {
        Class<?> clazz;
        if (javaType instanceof ParameterizedType) {
            clazz = (Class<?>) ((ParameterizedType) javaType).getRawType();
        } else {
            clazz = (Class<?>) javaType;
        }

        return Arrays.stream(clazz.getMethods())
                .filter(method -> Modifier.isAbstract(method.getModifiers()))
                .map(sam -> samProcessor.process(javaType, sam, ctx))
                .findFirst();
    }

    private boolean isValid(Class javaClass) {
        return !emitSAMs.equals(EmitSAMStrictness.byAnnotationOnly) || Arrays.stream(javaClass.getAnnotations())
                .anyMatch(a -> Objects.equals(a.annotationType(), FunctionalInterface.class));
    }

    private boolean shouldProcessParameterizedType(Type javaType) {
        if (javaType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) javaType;
            return parameterizedType.getRawType() instanceof Class && isValid((Class<?>) parameterizedType.getRawType());
        }
        return false;
    }

    private Result processParameterizedSAM(Type javaType, Method sam, Context context) {
        ParameterizedType parameterizedType = (ParameterizedType) javaType;
        Class<?> javaClass = (Class<?>) parameterizedType.getRawType();
        List<TypeVariable<? extends Class<?>>> typeVariables = Arrays.asList(javaClass.getTypeParameters());
        List<Type> typeArguments = Arrays.asList(parameterizedType.getActualTypeArguments());
        Iterator<Type> itor = typeArguments.iterator();
        Map<String, Type> genericTypeMap = typeVariables.stream().collect(toMap(TypeVariable::getName, v -> itor.next()));

        String genericReturnType = sam.getGenericReturnType().getTypeName();
        TsType returnType = context.processType(genericTypeMap.getOrDefault(genericReturnType, sam.getReturnType())).getTsType();

        List<TsParameter> parameters = new ArrayList<>();
        for (Type type : sam.getGenericParameterTypes()) {
            parameters.add(new TsParameter("arg" + parameters.size(),
                                           context.processType(genericTypeMap.getOrDefault(type.getTypeName(), type)).getTsType()));
        }

        return new Result(new TsType.FunctionType(parameters, returnType));
    }

    private boolean shouldProcessNonParameterizedType(Type javaType) {
        return emitSAMs.equals(EmitSAMStrictness.byAnnotationOnly) && javaType instanceof Class<?> &&
                isValid((Class<?>) javaType);
    }

    private Result processNonParameterizedSAM(Type type, Method method, Context context) {
        List<TsParameter> parameters = new ArrayList<>();
        for (Type paramType : method.getParameterTypes()) {
            parameters.add(new TsParameter("arg" + parameters.size(), context.processType(paramType).getTsType()));
        }
        return new Result(new TsType.FunctionType(parameters, context.processType(method.getReturnType()).getTsType()));
    }

    interface SAMProcessor {
        Result process(Type type, Method method, Context context);
    }

}
