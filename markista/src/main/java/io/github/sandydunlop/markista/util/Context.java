package io.github.sandydunlop.markista.util;

import javax.tools.Diagnostic;

import jdk.javadoc.doclet.Reporter;

public class Context {
    private static Reporter reporter;
    private static String moduleName = "";
    private static String packageName = "";
    private static String typeName = "";
    private static String methodName = "";
    private static String fieldName = "";

    private Context() {
        // No public constructor here
    }
    
    public static void setReporter(Reporter r) {
        reporter = r;
    }

    public static Reporter getReporter() {
        return reporter;
    }

    public static void setModuleName(String name) {
        setPackageName("");
        moduleName = name;
    }

    public static String getModuleName() {
        return moduleName;
    }

    public static void setPackageName(String name) {
        setTypeName("");
        packageName = name;
    }

    public static String getPackageName() {
        return packageName;
    }

    public static void setTypeName(String name) {
        typeName = name;
    }

    public static String getTypeName() {
        return typeName;
    }

    public static void setMethodName(String name) {
        methodName = name;
    }

    public static String getMethodName() {
        return methodName;
    }

    public static void setFieldName(String name) {
        fieldName = name;
    }

    public static String getFieldName() {
        return fieldName;
    }

    public static void reportInfo(String message) {
        reporter.print(Diagnostic.Kind.NOTE, message);
    }

    public static void reportWarning(String message) {
        reporter.print(Diagnostic.Kind.WARNING, message + location());
    }

    public static void reportError(String message) {
        reporter.print(Diagnostic.Kind.ERROR, message + location());
    }

    private static String location() {
        StringBuilder sb = new StringBuilder();
        if (!moduleName.isEmpty()) {
            sb.append("    [ Module] ");
            sb.append(moduleName);
            sb.append("\n");
        }
        if (!packageName.isEmpty()) {
            sb.append("    [Package] ");
            sb.append(packageName);
            sb.append("\n");
        }
        if (!typeName.isEmpty()) {
            sb.append("    [   Type] ");
            sb.append(typeName);
            sb.append("\n");
        }
        if (!methodName.isEmpty()) {
            sb.append("    [ Method] ");
            sb.append(methodName);
            sb.append("\n");
        }
        if (!fieldName.isEmpty()) {
            sb.append("    [  Field] ");
            sb.append(fieldName);
            sb.append("\n");
        }
        if (!sb.isEmpty()) {
            return "\n  Location:\n" + sb.toString();
        } else {
            return "";
        }
    }
}
