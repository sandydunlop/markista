package io.github.sandydunlop.markista.model;

public enum Modifier {
    PUBLIC ("public"),
    PROTECTED ("protected"),
    PRIVATE ("private"),
    STATIC ("static"),
    FINAL ("final"),
    ABSTRACT ("abstract"),
    SYNCHRONIZED ("synchronized"),
    TRANSIENT ("transient"),
    VOLATILE ("volatile"),
    NATIVE ("native"),
    STRICTFP ("strictfp"),
    DEFAULT ("default");

    private final String name;       

    private Modifier(String s) {
        name = s;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
