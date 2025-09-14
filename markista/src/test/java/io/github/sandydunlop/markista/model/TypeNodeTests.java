package io.github.sandydunlop.markista.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TypeNodeTests {
    @Test
    void bla() {
        TypeNode typeNode = new TypeNode(new Name("com.example.Test", "com.exmaple"));
        typeNode.addModifier(Modifier.STATIC);
        typeNode.addModifier(Modifier.FINAL);

        String actual = typeNode.getModifiersString();
        assertEquals("static final ", actual);
    }
}
