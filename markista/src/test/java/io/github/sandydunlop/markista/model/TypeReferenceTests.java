package io.github.sandydunlop.markista.model;

import io.github.sandydunlop.markista.markdown.MarkdownUtils;

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeReferenceTests {
    @Test
    void generics_list_of_string() {
        String code = "java.util.List<java.lang.String>";
        TypeReference typeRef = TypeReference.to(code);
        assertEquals("java.util.List", typeRef.getTypeString());
        assertInstanceOf(TypeReference.Generic.class, typeRef);

        TypeReference.Generic generic = typeRef.asGeneric();
        TypeReference params = generic.getParams();

        assertFalse(params instanceof TypeReference.Array);
        assertFalse(params instanceof TypeReference.Generic);
        assertFalse(params instanceof TypeReference.Sequence);
        assertEquals("java.lang.String", params.getTypeString());
    }

    @Test
    void generics_hashmap_of_string_string() {
        String code = "HashMap<String,String>";
        TypeReference typeRef = TypeReference.to(code);
        assertEquals("HashMap", typeRef.getTypeString());
        assertInstanceOf(TypeReference.Generic.class, typeRef);

        TypeReference.Generic generic = typeRef.asGeneric();
        TypeReference params = generic.getParams();

        assertTrue(params instanceof TypeReference.Sequence);
        TypeReference.Sequence sequence = params.asSequence();

        TypeReference param0 = sequence.getItems().get(0);
        assertEquals("String", param0.getTypeString());

        TypeReference param1 = sequence.getItems().get(0);
        assertEquals("String", param1.getTypeString());

    }

    @Test
    void generics_list_of_string_array() {
        String code = "java.util.List<java.lang.String[]>";
        TypeReference typeRef = TypeReference.to(code);
        assertEquals("java.util.List", typeRef.getTypeString());
        assertInstanceOf(TypeReference.Generic.class, typeRef);
        
        TypeReference.Generic generic = typeRef.asGeneric();
        TypeReference params = generic.getParams();

        assertTrue(params instanceof TypeReference.Array);

        TypeReference.Array array = params.asArray();
        assertEquals("java.lang.String", params.getTypeString());
        assertEquals(1, array.getDimensions());
    }

    @Test
    void generics_list_of_2D_string_array() {
        String code = "java.util.List<java.lang.String[][]>";
        TypeReference typeRef = TypeReference.to(code);
        assertEquals("java.util.List", typeRef.getTypeString());
        assertInstanceOf(TypeReference.Generic.class, typeRef);
        
        TypeReference.Generic generic = typeRef.asGeneric();
        TypeReference params = generic.getParams();

        assertTrue(params instanceof TypeReference.Array);

        TypeReference.Array array = params.asArray();
        assertEquals("java.lang.String", params.getTypeString());
        assertEquals(2, array.getDimensions());
    }

    @Test
    void generics_list_of_list_of_string() {
        String code = "java.util.List<java.util.List<java.lang.String>>";
        TypeReference typeRef = TypeReference.to(code);
        assertEquals("java.util.List", typeRef.getTypeString());
        assertInstanceOf(TypeReference.Generic.class, typeRef);
        
        TypeReference.Generic generic1 = typeRef.asGeneric();
        TypeReference params1 = generic1.getParams();
        assertEquals("java.util.List", params1.getTypeString());
        assertInstanceOf(TypeReference.Generic.class, params1);

        TypeReference.Generic generic2 = params1.asGeneric();
        TypeReference params2 = generic2.getParams();

        assertFalse(params2 instanceof TypeReference.Array);
        assertFalse(params2 instanceof TypeReference.Generic);
        assertFalse(params2 instanceof TypeReference.Sequence);
        assertEquals("java.lang.String", params2.getTypeString());
    }

    @Test
    void generics_long() {
        String code = "HashMap<TypeReference,List<Link>>";
        TypeReference typeRef = TypeReference.to(code);
        assertEquals("HashMap", typeRef.getTypeString());
        assertInstanceOf(TypeReference.Generic.class, typeRef);
        
        TypeReference.Generic generic1 = typeRef.asGeneric();
        TypeReference params = generic1.getParams();

        assertTrue(params instanceof TypeReference.Sequence);
        TypeReference.Sequence sequence = params.asSequence();

        TypeReference param0 = sequence.getItems().get(0);
        assertEquals("TypeReference", param0.getTypeString());

        TypeReference param1 = sequence.getItems().get(1);
        assertTrue(param1 instanceof TypeReference.Generic);

        TypeReference.Generic generic2 = param1.asGeneric();
        assertEquals("List", generic2.getTypeString());

        TypeReference param2 = generic2.getParams();
        assertEquals("Link", param2.getTypeString());

        String s = MarkdownUtils.formatTypeRef(typeRef);
        assertEquals("HashMap<TypeReference, List<Link>>", s);
    }

    @Test
    void generics_set_of_extends() {
        String code = "java.util.Set<? extends io.github.sandydunlop.markista.doclet.MarkdownDoclet.Option>";
        TypeReference typeRef = TypeReference.to(code);
        assertEquals("java.util.Set", typeRef.getTypeString());
        assertInstanceOf(TypeReference.Generic.class, typeRef);
        
        TypeReference.Generic generic = typeRef.asGeneric();
        assertTrue(generic.hasWildcard());

        TypeReference params = generic.getParams();
        assertFalse(params instanceof TypeReference.Array);
        assertFalse(params instanceof TypeReference.Generic);
        assertFalse(params instanceof TypeReference.Sequence);
    }
}
