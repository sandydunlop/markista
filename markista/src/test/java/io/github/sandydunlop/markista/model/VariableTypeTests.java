package io.github.sandydunlop.markista.model;

import io.github.sandydunlop.markista.markdown.MarkdownUtils;
import io.github.sandydunlop.markista.model.VariableType.BoundingKind;
import io.github.sandydunlop.markista.model.VariableType.Generic;
import io.github.sandydunlop.markista.model.VariableType.Sequence;
import io.github.sandydunlop.markista.model.VariableType.TypeParameter;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VariableTypeTests {
    @Test
    void generics_list_of_string() {
        String code = "java.util.List<java.lang.String>";
        VariableType typeRef = VariableType.parse(code);
        assertEquals("java.util.List", typeRef.getRawTypeName());
        assertInstanceOf(VariableType.Generic.class, typeRef);

        VariableType.Generic generic = typeRef.asGeneric();
        VariableType params = generic.getParams();

        assertFalse(params.isArray());
        assertFalse(params instanceof VariableType.Generic);
        assertFalse(params instanceof VariableType.Sequence);
        assertEquals("java.lang.String", params.getRawTypeName());
    }

    @Test
    void generics_hashmap_of_string_string() {
        String code = "HashMap<String,String>";
        VariableType typeRef = VariableType.parse(code);
        assertEquals("HashMap", typeRef.getRawTypeName());
        assertInstanceOf(VariableType.Generic.class, typeRef);

        VariableType.Generic generic = typeRef.asGeneric();
        VariableType params = generic.getParams();

        assertTrue(params instanceof VariableType.Sequence);
        VariableType.Sequence sequence = params.asSequence();

        VariableType param0 = sequence.getFirst();
        assertEquals("String", param0.getRawTypeName());

        VariableType param1 = sequence.getFirst();
        assertEquals("String", param1.getRawTypeName());

    }

    @Test
    void generics_list_of_string_array() {
        String code = "java.util.List<java.lang.String[]>";
        VariableType typeRef = VariableType.parse(code);
        assertEquals("java.util.List", typeRef.getRawTypeName());
        assertInstanceOf(VariableType.Generic.class, typeRef);

        VariableType.Generic generic = typeRef.asGeneric();
        VariableType params = generic.getParams();

        assertTrue(params instanceof TypeParameter);
        assertTrue(params.isArray());
        assertEquals("java.lang.String", params.getRawTypeName());
        assertEquals(1, params.arrayDimensions());
    }

    @Test
    void generics_list_of_2D_string_array() {
        String code = "java.util.List<java.lang.String[][]>";
        VariableType typeRef = VariableType.parse(code);
        assertEquals("java.util.List", typeRef.getRawTypeName());
        assertInstanceOf(VariableType.Generic.class, typeRef);

        VariableType.Generic generic = typeRef.asGeneric();
        VariableType params = generic.getParams();

        assertTrue(params.isArray());
        assertEquals("java.lang.String", params.getRawTypeName());
        assertEquals(2, params.arrayDimensions());
    }

    @Test
    void generics_list_of_list_of_string() {
        String code = "java.util.List<java.util.List<java.lang.String>>";
        VariableType typeRef = VariableType.parse(code);
        assertEquals("java.util.List", typeRef.getRawTypeName());
        assertInstanceOf(VariableType.Generic.class, typeRef);

        VariableType.Generic generic1 = typeRef.asGeneric();
        VariableType params1 = generic1.getParams();
        assertEquals("java.util.List", params1.getRawTypeName());
        assertInstanceOf(VariableType.Generic.class, params1);

        VariableType.Generic generic2 = params1.asGeneric();
        VariableType params2 = generic2.getParams();

        assertFalse(params2.isArray());
        assertFalse(params2 instanceof VariableType.Generic);
        assertFalse(params2 instanceof VariableType.Sequence);
        assertEquals("java.lang.String", params2.getRawTypeName());
    }

    @Test
    void generics_long() {
        String code = "HashMap<VariableType,List<Link>>";
        VariableType typeRef = VariableType.parse(code);
        assertEquals("HashMap", typeRef.getRawTypeName());
        assertInstanceOf(VariableType.Generic.class, typeRef);

        VariableType.Generic generic1 = typeRef.asGeneric();
        VariableType params = generic1.getParams();

        assertInstanceOf(VariableType.Sequence.class, params);
        VariableType.Sequence sequence = params.asSequence();

        VariableType param0 = sequence.getFirst();
        assertEquals("VariableType", param0.getRawTypeName());

        VariableType param1 = sequence.get(1);
        assertInstanceOf(VariableType.Generic.class, param1);

        VariableType.Generic generic2 = param1.asGeneric();
        assertEquals("List", generic2.getRawTypeName());

        VariableType param2 = generic2.getParams();
        assertEquals("Link", param2.getRawTypeName());

        String s = MarkdownUtils.formatVariableType(typeRef);
        assertEquals("HashMap<VariableType, List<Link>>", s);
    }

    @Test
    void generics_set_of_extends() {
        String code = "java.util.Set<? extends io.github.sandydunlop.markista.doclet.MarkdownDoclet.Option>";
        VariableType typeRef = VariableType.parse(code);
        assertEquals("java.util.Set", typeRef.getRawTypeName());
        assertInstanceOf(VariableType.Generic.class, typeRef);

        assertEquals(code, typeRef.toString());

        VariableType.Generic generic = typeRef.asGeneric();
        TypeParameter param = generic.getParams();
        assertEquals(BoundingKind.UPPER, generic.getBoundingKind());
        assertEquals("?", generic.getBoundingParameter());

        assertFalse(param.isArray());
        assertFalse(param instanceof VariableType.Generic);
        assertFalse(param instanceof VariableType.Sequence);
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
        VariableType typeRef = VariableType.parse(code);
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
        VariableType typeRef = VariableType.parse(code);
        assertEquals(code, typeRef.toString());
    }

    @Test
    void generics_array_param() {
        String code = "Map<String,String[]>";
        VariableType map = VariableType.parse(code);
        assertEquals(code, map.toString());
        TypeParameter param2 = map.asGeneric().getParams().asSequence().getLast();
        assertTrue(param2.isArray());
    }

    @Test
    void generics_throws_exception() {
        String code = "Map<String,String[]>";
        VariableType map = VariableType.parse(code);
        assertEquals(code, map.toString());
        Sequence params = map.asGeneric().getParams().asSequence();
        assertThrows(NoSuchElementException.class, () -> params.get(2));
    }

    @Test
    void generics_throws_exception2() {
        String code = "Map<String,String[]>";
        VariableType map = VariableType.parse(code);
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
        VariableType type = VariableType.parse(code);
        assertEquals("String", type.toString());
    }

    @Test
    void no_name() {
        VariableType type = VariableType.parse("");
        assertEquals("", type.toString());

        type = VariableType.parse(null);
        assertEquals("", type.toString());
    }

    @Test
    void primitive() {
        String code = "int";
        VariableType type = VariableType.parse(code);
        assertEquals("int", type.toString());
    }

    @Test
    void generics_list_of_wildcard_extends_array_type() {
        String code = "List<? extends TypeParameter[]>";
        VariableType type = VariableType.parse(code);
        assertEquals(code, type.toString());
    }

    @Test
    void generics_array_of_lists() {
        String code = "List<TypeParameter>[]";
        VariableType type = VariableType.parse(code);
        String s = type.toString();
        assertEquals(code, s);
    }

    @Test
    void generics_array_of_lists_of_wildcard() {
        String code = "List<?>[]";
        VariableType type = VariableType.parse(code);
        String s = type.toString();
        assertEquals(code, s);
    }

    @Test
    void genericCollection_T_extends_B() {
        String code = "Collection<T extends B>";
        VariableType type = VariableType.parse(code);
        String s = type.toString();
        assertEquals(code, s);
    }

    @Test
    void genericCollection_wildcard_super_B() {
        String code = "Collection<? super B>";
        VariableType string = VariableType.parse(code);
        String s = string.toString();
        assertEquals(code, s);
    }

    @Test
    void test_A_extends_type_A() {
        String code = "<A extends java.lang.annotation.Annotation> A";
        VariableType type = VariableType.parse(code);
        String s = type.toString();
        assertEquals(code, s);
    }

    @Test
    void genericMethodReturnType() {
        String code = "<E> java.util.List<E>";
        VariableType type = VariableType.parse(code);
        String s = type.toString();
        assertEquals(code, s);
    }

    @Test
    void test_T_extends_type_T_T() {
        String code = "<T extends java.lang.Enum<T>> T";
        VariableType type = VariableType.parse(code);
        String s = type.toString();
        assertEquals(code, s);
    }

    @Test
    void test_e() {
        String code = "java.util.Map<? extends javax.lang.model.element.ExecutableElement,? extends javax.lang.model.element.AnnotationValue>";
        VariableType type = VariableType.parse(code);
        String s = type.toString();
        assertEquals(code, s);
    }

    @Test
    void test_f() {
        String code = "p.Modeller<jl.Module,jl.Package,jl.Class<?>,jl.reflect.Field,jl.reflect.Method,jl.reflect.Parameter>";
        VariableType type = VariableType.parse(code);
        String s = type.toString();
        assertEquals(code, s);
    }

    @Test
    void test_wildcard_super_T() {
        String code = "java.lang.Class<? super T>";
        VariableType type = VariableType.parse(code);
        String s = type.toString();
        assertEquals(code, s);
    }
}
