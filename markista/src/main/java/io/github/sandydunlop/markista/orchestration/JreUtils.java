package io.github.sandydunlop.markista.orchestration;

import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.Modifier;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.TypeNode;
import io.github.sandydunlop.markista.modelling.StandardModeller;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;

public class JreUtils {
    protected JreUtils() {
        // Nothing to see here
    }

    public static Class<?> loadClass(String qualifiedName) {
        Optional<Class<?>> candidate = tryLoadClass(qualifiedName);
        if (candidate.isPresent()) {
            return candidate.get();
        }
        Name name = new Name(qualifiedName);
        for (int split = name.size() - 1; split>0; split--) {
            String start = name.first(split);
            String end = name.last(name.size() - split);
            qualifiedName = start + "$" + end;
            candidate = tryLoadClass(qualifiedName);
            if (candidate.isPresent()) {
                return candidate.get();
            }
        }
        return null;
    }

    private static Optional<Class<?>> tryLoadClass(String qualifiedName) {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try {
            Class<?> loadedClass = classLoader.loadClass(qualifiedName);
            return Optional.of(loadedClass);
        } catch (ClassNotFoundException _) {
            // Ignore it
        }
        return Optional.empty();
    }

    public static MethodNode[] getMethods(Class<?> jreClass) {
        StandardModeller modeller = new StandardModeller();
        Method[] methods = jreClass.getMethods();
        MethodNode[] methodNodes = new MethodNode[methods.length];
        for (int i=0; i<methods.length; i++) {
            methodNodes[i] = modeller.modelMethod(methods[i]);
        }
        return methodNodes;
    }

    // public static TypeNode model(Class<?> type) {
    //     TypeNode typeNode = new TypeNode(type.getSimpleName(), type.getPackageName());
    //     Method[] methods = type.getMethods();
    //     for (Method method : methods) {
    //         MethodNode methodNode = model(method);
    //         typeNode.addMethod(methodNode);
    //     }
    //     return typeNode;
    // }

    // public static MethodNode model(Method method) {
    //     MethodNode methodNode = parseMethodString(method.toGenericString());
    //     for (Parameter param : method.getParameters()) {
    //         ParamNode paramNode = new ParamNode(param.getType().getName(), param.getName());
    //         methodNode.addParam(paramNode);
    //     }
    //     return methodNode;
    // }

    // private static MethodNode parseMethodString(String method) {
    //     int openParenthesis = method.indexOf("(");
    //     String modifiersTypeAndName = method.substring(0, openParenthesis);
    //     int pos = modifiersTypeAndName.lastIndexOf(" ");
    //     String qualifiedName = modifiersTypeAndName.substring(pos);
    //     String modifiersAndType = modifiersTypeAndName.substring(0, pos);
    //     pos = qualifiedName.lastIndexOf(".");
    //     String simpleName = qualifiedName.substring(pos + 1);
    //     String returnType = "";
    //     StringBuilder modifiers = new StringBuilder();
    //     String[] parts = modifiersAndType.split(" ");
    //     pos = 0;
    //     while (pos < parts.length) {
    //         String part = parts[pos];
    //         Modifier mod = Modifier.DEFAULT;
    //         try{
    //             mod = Modifier.valueOf(part.toUpperCase());
    //             if (mod != Modifier.DEFAULT) {
    //                 if (!modifiers.isEmpty()) {
    //                     modifiers.append(" ");
    //                 }
    //                 modifiers.append(mod.name());
    //             }
    //         }catch(IllegalArgumentException _) {
    //             // Do nothing
    //             returnType = modifiersAndType.substring(modifiers.length()).strip();
    //         }
    //         pos++;
    //     }
    //     MethodNode methodNode = new MethodNode(returnType, simpleName);
    //     parts = modifiers.toString().split(" ");
    //     for (String part : parts) {
    //         methodNode.addModifier(Modifier.valueOf(part));
    //     }
    //     return methodNode;
    // }
}
