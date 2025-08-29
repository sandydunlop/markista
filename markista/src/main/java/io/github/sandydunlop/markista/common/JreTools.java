package io.github.sandydunlop.markista.common;


import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;

public class JreTools {
    protected JreTools() {
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

    public static boolean typeHasMethod(Class<?> type, String signature) {
        Method[] methods = type.getMethods();
        for (Method m : methods) {
            String candidate = methodSignature(m);
            if (candidate.equals(signature)) {
                return true;
            }
        }
        return false;
    }

    public static String methodSignature(Method method) {
        StringBuilder sb = new StringBuilder();
        for (Parameter param : method.getParameters()) {
            if (!sb.isEmpty()) {
                sb.append(",");
            }
            Class<?> paramType = param.getType();
            sb.append(paramType.getCanonicalName());
        }
        return method.getName() + "(" + sb + ")";
    }
}
