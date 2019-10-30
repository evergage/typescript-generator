
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.compiler.Symbol;
import cz.habarta.typescript.generator.compiler.SymbolTable;

import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.*;


public interface TypeProcessor {

    /**
     * @return <code>null</code> if this processor didn't process passed java type
     */
    public Result processType(Type javaType, Context context);

    public default Result processTypeInTemporaryContext(Type type, Member member, Object typeContext, Settings settings) {
        return processType(type, new Context(new SymbolTable(settings), this, member, typeContext));
    }

    public default List<Class<?>> discoverClassesUsedInType(Type type, Member member, Object typeContext, Settings settings) {
        final TypeProcessor.Result result = processTypeInTemporaryContext(type, member, typeContext, settings);
        return result != null ? result.getDiscoveredClasses() : Collections.emptyList();
    }

    public default boolean isTypeExcluded(Type type, Object typeContext, Settings settings) {
        final TypeProcessor.Result result = processTypeInTemporaryContext(type, null, typeContext, settings);
        return result != null && result.tsType == TsType.Any;
    }

    public static class Context {

        private final SymbolTable symbolTable;
        private final TypeProcessor typeProcessor;
        private final Object typeContext;
        private final Member member;

        public Context(SymbolTable symbolTable, TypeProcessor typeProcessor, Member member, Object typeContext) {
            this.symbolTable = Objects.requireNonNull(symbolTable, "symbolTable");
            this.typeProcessor = Objects.requireNonNull(typeProcessor, "typeProcessor");
            this.member = member;
            this.typeContext = typeContext;
        }

        public Symbol getSymbol(Class<?> cls) {
            return symbolTable.getSymbol(cls);
        }

        public Result processType(Type javaType) {
            return typeProcessor.processType(javaType, this);
        }

        public Object getTypeContext() {
            return typeContext;
        }

        public Context withTypeContext(Object typeContext) {
            return new Context(symbolTable, typeProcessor, member, typeContext);
        }

        public Member getMember() {
            return member;
        }
    }

    public static class Result {

        private final TsType tsType;
        private final List<Class<?>> discoveredClasses;

        public Result(TsType tsType, List<Class<?>> discoveredClasses) {
            this.tsType = tsType;
            this.discoveredClasses = discoveredClasses != null ? discoveredClasses : Collections.emptyList();
        }

        public Result(TsType tsType, Class<?>... discoveredClasses) {
            this.tsType = tsType;
            this.discoveredClasses = discoveredClasses != null ? Arrays.asList(discoveredClasses) : Collections.emptyList();
        }

        public TsType getTsType() {
            return tsType;
        }

        public List<Class<?>> getDiscoveredClasses() {
            return discoveredClasses;
        }

    }

    public static class Chain implements TypeProcessor {

        private final List<TypeProcessor> processors;

        public Chain(List<TypeProcessor> processors) {
            this.processors = processors;
        }

        public Chain(TypeProcessor... processors) {
            this.processors = Arrays.asList(processors);
        }

        @Override
        public Result processType(Type javaType, Context context) {
            for (TypeProcessor processor : processors) {
                final Result result = processor.processType(javaType, context);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }

    }

}
