package io.github.sandydunlop.markista.orchestration;

public class Name {
    String[] components;

    public Name(String n) {
        components = n.split("\\.");
    }

    public String[] components() {
        return components;
    }

    public int size() {
        return components.length;
    }

    public String first(int n) {
        if (n > components.length) {
            throw new IndexOutOfBoundsException(String.format(
                    "%d is out of bounds for %d", n, components.length));
        }
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<n; i++) {
            if (!sb.isEmpty()) {
                sb.append(".");
            }
            sb.append(components[i]);
        }
        return sb.toString();
    }

    public String last(int n) {
        if (n > components.length) {
            throw new IndexOutOfBoundsException(String.format(
                    "%d is out of bounds for %d", n, components.length));
        }
        StringBuilder sb = new StringBuilder();
        for (int i=components.length - n; i<components.length; i++) {
            if (!sb.isEmpty()) {
                sb.append(".");
            }
            sb.append(components[i]);
        }
        return sb.toString();
    }
}
