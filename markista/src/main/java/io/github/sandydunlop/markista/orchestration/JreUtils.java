package io.github.sandydunlop.markista.orchestration;

import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.Name;
import io.github.sandydunlop.markista.modelling.StandardModeller;

import java.lang.reflect.Method;
import java.util.Optional;

public class JreUtils {
    static Context ctx;

    protected JreUtils() {
        // Nothing to see here
    }

    public static Class<?> loadClass(String qualifiedName) {
        Optional<Class<?>> candidate = tryLoadClass(qualifiedName);
        if (candidate.isPresent()) {
            return candidate.get();
        }
        Name name = new Name(qualifiedName);
        for (int split = name.componentCount() - 1; split>0; split--) {
            name.setPackageComponentCount(split);
            qualifiedName = name.fullyQualifiedBinaryName();
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
}
