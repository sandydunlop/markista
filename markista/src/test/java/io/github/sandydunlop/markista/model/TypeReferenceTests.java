package io.github.sandydunlop.markista.model;

import io.github.sandydunlop.markista.markdown.MarkdownUtils;
import io.github.sandydunlop.markista.model.TypeReference.Generic;
import io.github.sandydunlop.markista.model.TypeReference.Sequence;
import io.github.sandydunlop.markista.model.TypeReference.TypeParameter;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeReferenceTests {
    @Test
    void generics_list_of_string() {
        String code = "java.util.List<java.lang.String>";
        TypeReference typeRef = TypeReference.to(code);
        assertEquals("java.util.List", typeRef.getRawTypeName());
        assertInstanceOf(TypeReference.Generic.class, typeRef);

        TypeReference.Generic generic = typeRef.asGeneric();
        TypeReference params = generic.getParams();

        assertFalse(params.isArray());
        assertFalse(params instanceof TypeReference.Generic);
        assertFalse(params instanceof TypeReference.Sequence);
        assertEquals("java.lang.String", params.getRawTypeName());
    }

    @Test
    void generics_hashmap_of_string_string() {
        String code = "HashMap<String,String>";
        TypeReference typeRef = TypeReference.to(code);
        assertEquals("HashMap", typeRef.getRawTypeName());
        assertInstanceOf(TypeReference.Generic.class, typeRef);

        TypeReference.Generic generic = typeRef.asGeneric();
        TypeReference params = generic.getParams();

        assertTrue(params instanceof TypeReference.Sequence);
        TypeReference.Sequence sequence = params.asSequence();

        TypeReference param0 = sequence.getFirst();
        assertEquals("String", param0.getRawTypeName());

        TypeReference param1 = sequence.getFirst();
        assertEquals("String", param1.getRawTypeName());

    }

    @Test
    void generics_list_of_string_array() {
        String code = "java.util.List<java.lang.String[]>";
        TypeReference typeRef = TypeReference.to(code);
        assertEquals("java.util.List", typeRef.getRawTypeName());
        assertInstanceOf(TypeReference.Generic.class, typeRef);

        TypeReference.Generic generic = typeRef.asGeneric();
        TypeReference params = generic.getParams();

        assertTrue(params instanceof TypeParameter);
        assertTrue(params.isArray());
        assertEquals("java.lang.String", params.getRawTypeName());
        assertEquals(1, params.arrayDimensions());
    }

    @Test
    void generics_list_of_2D_string_array() {
        String code = "java.util.List<java.lang.String[][]>";
        TypeReference typeRef = TypeReference.to(code);
        assertEquals("java.util.List", typeRef.getRawTypeName());
        assertInstanceOf(TypeReference.Generic.class, typeRef);

        TypeReference.Generic generic = typeRef.asGeneric();
        TypeReference params = generic.getParams();

        assertTrue(params.isArray());
        assertEquals("java.lang.String", params.getRawTypeName());
        assertEquals(2, params.arrayDimensions());
    }

    @Test
    void generics_list_of_list_of_string() {
        String code = "java.util.List<java.util.List<java.lang.String>>";
        TypeReference typeRef = TypeReference.to(code);
        assertEquals("java.util.List", typeRef.getRawTypeName());
        assertInstanceOf(TypeReference.Generic.class, typeRef);

        TypeReference.Generic generic1 = typeRef.asGeneric();
        TypeReference params1 = generic1.getParams();
        assertEquals("java.util.List", params1.getRawTypeName());
        assertInstanceOf(TypeReference.Generic.class, params1);

        TypeReference.Generic generic2 = params1.asGeneric();
        TypeReference params2 = generic2.getParams();

        assertFalse(params2.isArray());
        assertFalse(params2 instanceof TypeReference.Generic);
        assertFalse(params2 instanceof TypeReference.Sequence);
        assertEquals("java.lang.String", params2.getRawTypeName());
    }

    @Test
    void generics_long() {
        String code = "HashMap<TypeReference,List<Link>>";
        TypeReference typeRef = TypeReference.to(code);
        assertEquals("HashMap", typeRef.getRawTypeName());
        assertInstanceOf(TypeReference.Generic.class, typeRef);

        TypeReference.Generic generic1 = typeRef.asGeneric();
        TypeReference params = generic1.getParams();

        assertInstanceOf(TypeReference.Sequence.class, params);
        TypeReference.Sequence sequence = params.asSequence();

        TypeReference param0 = sequence.getFirst();
        assertEquals("TypeReference", param0.getRawTypeName());

        TypeReference param1 = sequence.get(1);
        assertInstanceOf(TypeReference.Generic.class, param1);

        TypeReference.Generic generic2 = param1.asGeneric();
        assertEquals("List", generic2.getRawTypeName());

        TypeReference param2 = generic2.getParams();
        assertEquals("Link", param2.getRawTypeName());

        String s = MarkdownUtils.formatTypeRef(typeRef);
        assertEquals("HashMap<TypeReference, List<Link>>", s);
    }

    @Test
    void generics_set_of_extends() {
        String code = "java.util.Set<? extends io.github.sandydunlop.markista.doclet.MarkdownDoclet.Option>";
        TypeReference typeRef = TypeReference.to(code);
        assertEquals("java.util.Set", typeRef.getRawTypeName());
        assertInstanceOf(TypeReference.Generic.class, typeRef);

        assertEquals(code, typeRef.toString());

        TypeReference.Generic generic = typeRef.asGeneric();
        TypeParameter param = generic.getParams();
        assertTrue(param.hasExtendsWildcard());

        assertFalse(param.isArray());
        assertFalse(param instanceof TypeReference.Generic);
        assertFalse(param instanceof TypeReference.Sequence);
        assertEquals(null, param.asGeneric());
        assertEquals(null, param.asSequence());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
        "Set<List<HashMap<String,HashMap<String,List<?>>>>>",
        "Test<String,String,String,String,String>",
        "List<?>",
        "Map<String,List<?>>"
    })
    void generics_various_cases(String code) {
        TypeReference typeRef = TypeReference.to(code);
        assertEquals(code, typeRef.toString());

        // Additional checks for "Map<String,List<?>>"
        if ("Map<String,List<?>>".equals(code)) {
            assertInstanceOf(Generic.class, typeRef);
            Sequence mapParams = typeRef.asGeneric().getParams().asSequence();
            assertEquals(2, mapParams.size());
            Generic list = mapParams.getLast().asGeneric();
            assertEquals("List", list.getRawTypeName());
            assertNotNull(list.asGeneric().getParams());
            TypeParameter wildcard = list.asGeneric().getParams();
            assertNotNull(wildcard);
        }
    }

    @Test
    void generics_three_params_and_nested() {
        String code = "Map<String,Text,List<?>>";
        TypeReference typeRef = TypeReference.to(code);
        assertEquals(code, typeRef.toString());
    }

    @Test
    void generics_array_param() {
        String code = "Map<String,String[]>";
        TypeReference map = TypeReference.to(code);
        assertEquals(code, map.toString());
        TypeParameter param2 = map.asGeneric().getParams().asSequence().getLast();
        assertTrue(param2.isArray());
    }

    @Test
    void generics_throws_exception() {
        String code = "Map<String,String[]>";
        TypeReference map = TypeReference.to(code);
        assertEquals(code, map.toString());
        Sequence params = map.asGeneric().getParams().asSequence();
        assertThrows(NoSuchElementException.class, () -> params.get(2));
    }

    @Test
    void generics_throws_exception2() {
        String code = "Map<String,String[]>";
        TypeReference map = TypeReference.to(code);
        assertEquals(code, map.toString());
        Sequence params = map.asGeneric().getParams().asSequence();

        Iterator<TypeParameter> iterator = params.iterator();
        TypeParameter param1 = iterator.next();
        assertEquals("String", param1.getRawTypeName());
        iterator.next();

        assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test
    void non_generic() {
        String code = "String";
        TypeReference string = TypeReference.to(code);
        assertEquals("String", string.toString());
    }

    @Test
    void no_name() {
        TypeReference string = TypeReference.to("");
        assertEquals("", string.toString());

        string = TypeReference.to(null);
        assertEquals("", string.toString());
    }

    @Test
    void primitive() {
        String code = "int";
        TypeReference string = TypeReference.to(code);
        assertEquals("int", string.toString());
    }

    @Test
    void generics_list_of_wildcard_extends_array_type() {
        String code = "List<? extends TypeParameter[]>";
        TypeReference string = TypeReference.to(code);
        assertEquals(code, string.toString());
    }

    @Test
    void generics_array_of_lists() {
        String code = "List<TypeParameter>[]";
        TypeReference string = TypeReference.to(code);
        String s = string.toString();
        assertEquals(code, s);
    }

    @Test
    void generics_array_of_lists_of_wildcard() {
        String code = "List<?>[]";
        TypeReference string = TypeReference.to(code);
        String s = string.toString();
        assertEquals(code, s);
    }

    @Test
    void genericMethodReturnType() {
        String code = "<E> java.util.List<E>";
        TypeReference string = TypeReference.to(code);
        String s = string.toString();
        assertEquals(code, s);
    }
}
