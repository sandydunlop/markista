package io.github.sandydunlop.markista.model;

public class OverriddenMethodNode {
    private String qualifiedClassName = "";
    private String methodName = "";

    public OverriddenMethodNode(String qualifiedClassName, String methodName) {
        this.qualifiedClassName = qualifiedClassName;
        this.methodName = methodName;
    }

    public String getClassName() {
        return qualifiedClassName;
    }

    public String getMethodName() {
        return methodName;
    }
}
