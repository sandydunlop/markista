package io.github.sandydunlop.markista.modeller;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.Modifier;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.TypeNode;

public class StandardModeller implements Modeller<Class<?>, Field, Method, Parameter> {
    @Override
    public TypeNode modelType(Class<?> type) {
        TypeNode typeNode = new TypeNode(type.getSimpleName(), type.getPackageName());
        for (Field field : type.getFields()) {
            FieldNode node = modelField(field);
            typeNode.addField(node);
        }
        for (Method method : type.getMethods()) {
            MethodNode node = modelMethod(method);
            typeNode.addMethod(node);
        }
        return typeNode;
    }

    @Override
    public FieldNode modelField(Field field) {
        // Implement a basic modelField method, adjust as needed
        return new FieldNode(field.getType().getName(), field.getName());
    }

    @Override
    public MethodNode modelMethod(Method method) {
        MethodNode methodNode = parseMethodString(method.toGenericString());
        for (Parameter param : method.getParameters()) {
            ParamNode paramNode = modelParam(param);
            methodNode.addParam(paramNode);
        }
        return methodNode;
    }

    @Override
    public ParamNode modelParam(Parameter parameter) {
        // Implement a basic modelParam method, adjust as needed
        return new ParamNode(parameter.getType().getName(), parameter.getName());
    }

    private MethodNode parseMethodString(String method) {
        int openParenthesis = method.indexOf("(");
        String modifiersTypeAndName = method.substring(0, openParenthesis);
        int pos = modifiersTypeAndName.lastIndexOf(" ");
        String qualifiedName = modifiersTypeAndName.substring(pos);
        String modifiersAndType = modifiersTypeAndName.substring(0, pos);
        pos = qualifiedName.lastIndexOf(".");
        String simpleName = qualifiedName.substring(pos + 1);
        String returnType = "";
        StringBuilder modifiers = new StringBuilder();
        String[] parts = modifiersAndType.split(" ");
        pos = 0;
        while (pos < parts.length) {
            String part = parts[pos];
            Modifier mod = Modifier.DEFAULT;
            try{
                mod = Modifier.valueOf(part.toUpperCase());
                if (mod != Modifier.DEFAULT) {
                    if (!modifiers.isEmpty()) {
                        modifiers.append(" ");
                    }
                    modifiers.append(mod.name());
                }
            }catch(IllegalArgumentException _) {
                // Do nothing
                returnType = modifiersAndType.substring(modifiers.length()).strip();
            }
            pos++;
        }
        MethodNode methodNode = new MethodNode(returnType, simpleName);
        parts = modifiers.toString().split(" ");
        for (String part : parts) {
            methodNode.addModifier(Modifier.valueOf(part));
        }
        return methodNode;
    }
}
