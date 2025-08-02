package io.github.sandydunlop.markista.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import javax.tools.Diagnostic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jdk.javadoc.doclet.Reporter;

class ContextTests {

    private Reporter reporter;

    @BeforeEach
    void setup() {
        reporter = mock(Reporter.class);
        Context.setReporter(reporter);
        // Clear all context fields before each test
        Context.setModuleName("");
        Context.setPackageName("");
        Context.setTypeName("");
        Context.setMethodName("");
        Context.setFieldName("");
    }

    @Test
    void testSetAndGetModuleName() {
        Context.setModuleName("my.module");
        assertEquals("my.module", Context.getModuleName());
        // Setting module name resets package name
        Context.setPackageName("pkg.name");
        Context.setModuleName("new.module");
        assertEquals("", Context.getPackageName());
        assertEquals("new.module", Context.getModuleName());
    }

    @Test
    void testSetAndGetPackageName() {
        Context.setPackageName("pkg.name");
        assertEquals("pkg.name", Context.getPackageName());
        // Setting package name resets type name
        Context.setTypeName("TypeA");
        Context.setPackageName("pkg.new");
        assertEquals("pkg.new", Context.getPackageName());
        assertEquals("", Context.getTypeName());
    }

    @Test
    void testSetAndGetTypeName() {
        Context.setTypeName("TypeA");
        assertEquals("TypeA", Context.getTypeName());
    }

    @Test
    void testSetAndGetMethodName() {
        Context.setMethodName("methodX");
        assertEquals("methodX", Context.getMethodName());
    }

    @Test
    void testSetAndGetFieldName() {
        Context.setFieldName("fieldY");
        assertEquals("fieldY", Context.getFieldName());
    }

    @Test
    void testReportInfoCallsReporter() {
        String message = "Information";
        Context.reportInfo(message);
        verify(reporter, times(1)).print(Diagnostic.Kind.NOTE, message);
    }

    @Test
    void testReportWarningIncludesLocation() {
        String message = "Warning message";
        // Setup context fields to produce location info
        Context.setModuleName("mod1");
        Context.setPackageName("pkg1");
        Context.setTypeName("Type1");
        Context.setMethodName("method1");
        Context.setFieldName("field1");

        Context.reportWarning(message);

        // Capture the message passed to print
        verify(reporter, times(1)).print(eq(Diagnostic.Kind.WARNING), contains(message));
        verify(reporter).print(eq(Diagnostic.Kind.WARNING), argThat(s -> s.contains("mod1") &&
                                                                    s.contains("pkg1") &&
                                                                    s.contains("Type1") &&
                                                                    s.contains("method1") &&
                                                                    s.contains("field1")));
    }

    @Test
    void testReportErrorIncludesLocationWhenNoFieldsIsEmpty() {
        String message = "Error message";
        // Clear all fields
        Context.setModuleName("");
        Context.setPackageName("");
        Context.setTypeName("");
        Context.setMethodName("");
        Context.setFieldName("");

        Context.reportError(message);

        verify(reporter).print(Diagnostic.Kind.ERROR, message);
    }

    @Test
    void testReportErrorIncludesLocationWhenFieldsSet() {
        String message = "Error found";
        Context.setModuleName("moduleA");
        Context.setTypeName("TypeB");
        Context.setMethodName("methodC");

        Context.reportError(message);

        verify(reporter).print(eq(Diagnostic.Kind.ERROR), argThat(s -> s.contains(message) &&
                                                                   s.contains("moduleA") &&
                                                                   s.contains("TypeB") &&
                                                                   s.contains("methodC")));
    }
}