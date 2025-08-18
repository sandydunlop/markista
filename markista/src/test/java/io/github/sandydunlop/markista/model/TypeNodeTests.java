package io.github.sandydunlop.markista.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TypeNodeTests {
    @Test
    void bla() {
        TypeNode typeNode = new TypeNode("com.example.Test", "Test", "com.exmaple");
        typeNode.addModifier(Modifier.STATIC);
        typeNode.addModifier(Modifier.FINAL);

        String actual = typeNode.getModifiersString();
        assertEquals("static final ", actual);
    }
}
