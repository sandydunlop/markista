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
import io.github.sandydunlop.markista.model.Name;
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
        return new FieldNode(field.getType().getTypeName(), field.getName());
    }

    @Override
    public MethodNode modelMethod(Method method) {
        MethodNode methodNode = createMethodNode(method.toGenericString(), method.getDeclaringClass());
        for (Parameter param : method.getParameters()) {
            ParamNode paramNode = modelParam(param);
            methodNode.addParam(paramNode);
        }
        return methodNode;
    }

    @Override
    public ParamNode modelParam(Parameter parameter) {
        return new ParamNode(parameter.getType().getTypeName(), parameter.getName());
    }

    private TypeNode modelType(Class<?> type, Node.Kind kind) {
        TypeNode typeNode;
        Name name = new Name(type.getCanonicalName(), type.getPackageName());
        switch(kind) {
            case ANNOTATION:
                typeNode = new AnnotationNode(name);
                break;
            case CLASS:
                typeNode = new ClassNode(name);
                break;
            case ENUM:
                typeNode = new EnumNode(name);
                break;
            case INTERFACE:
                typeNode = new ClassNode(name);
                break;
            case RECORD:
                typeNode = new RecordNode(name);
                break;
            default:
                typeNode = new TypeNode(name);
        }

        typeNode.setName(new Name(type.getCanonicalName(), type.getPackageName()));
        typeNode.setModuleName(type.getModule().getName());

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

    private MethodNode createMethodNode(String method, Class<?> owner) {
        int openParenthesis = method.indexOf("(");
        String modifiersTypeAndName = method.substring(0, openParenthesis);
        int pos = modifiersTypeAndName.lastIndexOf(" ");
        String qualifiedName = modifiersTypeAndName.substring(pos + 1);
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
                if (!modifiers.isEmpty()) {
                    modifiers.append(" ");
                }
                modifiers.append(mod.name());
            }catch(IllegalArgumentException _) {
                returnType = modifiersAndType.substring(modifiers.length()).strip();
            }
            pos++;
        }
        Name name = new Name(simpleName, owner.getCanonicalName(), owner.getPackageName());
        MethodNode methodNode = new MethodNode(returnType, name);
        parts = modifiers.toString().split(" ");
        for (String part : parts) {
            methodNode.addModifier(Modifier.valueOf(part));
        }
        return methodNode;
    }
}
