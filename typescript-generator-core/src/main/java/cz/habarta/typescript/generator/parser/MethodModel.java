
package cz.habarta.typescript.generator.parser;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;


public class MethodModel {

    private final Class<?> originClass;
    private final Method method;
    private final String name;
    private final List<MethodParameterModel> parameters;
    private final Type returnType;
    private final List<String> comments;

    public MethodModel(Class<?> originClass, Method method, String name, List<MethodParameterModel> parameters, Type returnType, List<String> comments) {
        this.originClass = originClass;
        this.method = method;
        this.name = name;
        this.parameters = parameters != null ? parameters : Collections.<MethodParameterModel>emptyList();
        this.returnType = returnType;
        this.comments = comments;
    }

    public Class<?> getOriginClass() {
        return originClass;
    }

    public Method getMethod() {
        return method;
    }

    public String getName() {
        return name;
    }

    public List<MethodParameterModel> getParameters() {
        return parameters;
    }

    public Type getReturnType() {
        return returnType;
    }

    public List<String> getComments() {
        return comments;
    }

    public MethodModel withParameters(List<MethodParameterModel> parameters) {
        return new MethodModel(originClass, method, name, parameters, returnType, comments);
    }

    public MethodModel withComments(List<String> comments) {
        return new MethodModel(originClass, method, name, parameters, returnType, comments);
    }
}
