package io.github.sandydunlop.markista.model;

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeRefTests {
    @Test
    void generics_list_of_string() {
        String code = "java.util.List<java.lang.String>";
        TypeRef typeRef = TypeRef.to(code);
        assertEquals("java.util.List", typeRef.getTypeString());
        assertInstanceOf(TypeRef.Generic.class, typeRef);

        TypeRef.Generic generic = typeRef.asGeneric();
        TypeRef params = generic.getParams();

        assertFalse(params instanceof TypeRef.Array);
        assertFalse(params instanceof TypeRef.Generic);
        assertFalse(params instanceof TypeRef.Sequence);
        assertEquals("java.lang.String", params.getTypeString());
    }

    @Test
    void generics_list_of_string_array() {
        String code = "java.util.List<java.lang.String[]>";
        TypeRef typeRef = TypeRef.to(code);
        assertEquals("java.util.List", typeRef.getTypeString());
        assertInstanceOf(TypeRef.Generic.class, typeRef);
        
        TypeRef.Generic generic = typeRef.asGeneric();
        TypeRef params = generic.getParams();

        assertTrue(params instanceof TypeRef.Array);

        TypeRef.Array array = params.asArray();
        assertEquals("java.lang.String", params.getTypeString());
        assertEquals(1, array.getDimensions());
    }

    @Test
    void generics_list_of_2D_string_array() {
        String code = "java.util.List<java.lang.String[][]>";
        TypeRef typeRef = TypeRef.to(code);
        assertEquals("java.util.List", typeRef.getTypeString());
        assertInstanceOf(TypeRef.Generic.class, typeRef);
        
        TypeRef.Generic generic = typeRef.asGeneric();
        TypeRef params = generic.getParams();

        assertTrue(params instanceof TypeRef.Array);

        TypeRef.Array array = params.asArray();
        assertEquals("java.lang.String", params.getTypeString());
        assertEquals(2, array.getDimensions());
    }

    @Test
    void generics_list_of_list_of_string() {
        String code = "java.util.List<java.util.List<java.lang.String>>";
        TypeRef typeRef = TypeRef.to(code);
        assertEquals("java.util.List", typeRef.getTypeString());
        assertInstanceOf(TypeRef.Generic.class, typeRef);
        
        TypeRef.Generic generic1 = typeRef.asGeneric();
        TypeRef params1 = generic1.getParams();
        assertEquals("java.util.List", params1.getTypeString());
        assertInstanceOf(TypeRef.Generic.class, params1);

        TypeRef.Generic generic2 = params1.asGeneric();
        TypeRef params2 = generic2.getParams();

        assertFalse(params2 instanceof TypeRef.Array);
        assertFalse(params2 instanceof TypeRef.Generic);
        assertFalse(params2 instanceof TypeRef.Sequence);
        assertEquals("java.lang.String", params2.getTypeString());
    }
}
