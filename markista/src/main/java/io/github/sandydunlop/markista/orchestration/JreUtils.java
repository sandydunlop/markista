package io.github.sandydunlop.markista.orchestration;

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
}
