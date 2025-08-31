package io.github.sandydunlop.markista.modelling;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import io.github.sandydunlop.markista.model.AnnotationNode;
import io.github.sandydunlop.markista.model.ClassNode;
import io.github.sandydunlop.markista.model.EnumNode;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.Modifier;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.Node;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.RecordNode;
import io.github.sandydunlop.markista.model.TypeNode;

public class StandardModeller implements Modeller<Module, Package, Class<?>, Field, Method, Parameter> {
    @Override
    public ModuleNode modelModule(Module m) {
        return null;
    }

    @Override
    public PackageNode modelPackage(Package p) {
        return null;
    }

    @Override
    public TypeNode modelType(Class<?> type) {
        return modelType(type, Node.Kind.NONE);
    }

    @Override
    public ClassNode modelClass(Class<?> type) {
        return (ClassNode)modelType(type, Node.Kind.CLASS);
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

    private TypeNode modelType(Class<?> type, Node.Kind kind) {
        TypeNode typeNode;
        switch(kind) {
            case ANNOTATION:
                typeNode = new AnnotationNode(type.getSimpleName(), type.getPackageName());
                break;
            case CLASS:
                typeNode = new ClassNode(type.getSimpleName(), type.getPackageName());
                break;
            case ENUM:
                typeNode = new EnumNode(type.getSimpleName(), type.getPackageName());
                break;
            case INTERFACE:
                typeNode = new ClassNode(type.getSimpleName(), type.getPackageName());
                break;
            case RECORD:
                typeNode = new RecordNode(type.getSimpleName(), type.getPackageName());
                break;
            default:
                typeNode = new TypeNode(type.getSimpleName(), type.getPackageName());
        }
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
