package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

/// Contains information about a method being documented
public class MethodNode extends Node {
    /// Description of the method's return value.
    private Text returnDescription = Text.empty();

    /// Name of the interface or specification this method is specified by.
    private String specifiedBy = "";

    /// The return type of this method.
    private TypeNode returnType = null;

    /// Information about the method that this method overrides, if any.
    private OverriddenMethodNode overrides = null;

    /// List of parameters for this method.
    private List<ParamNode> params = new ArrayList<>();

    /// List of exception types that this method declares it can throw.
    private List<String> thrownTypes = new ArrayList<>();

    /// The type (class/interface) that owns this method.
    private TypeNode owner = null;

    /// Constructs a MethodNode with the specified return type and method name.
    /// @param returnType the return type of the method.
    /// @param name       the simple name of the method.
    public MethodNode(TypeNode returnType, String name) {
        this.returnType = returnType;
        this.simpleName = name;
    }

    /// Sets the overridden method information that this method overrides.
    /// @param overrides an OverriddenMethodNode representing the overridden method.
    public void setOverriddenMethod(OverriddenMethodNode overrides) {
        this.overrides = overrides;
    }

    /// Returns the overridden method information, if any.
    /// @return the OverriddenMethodNode representing the overridden method, or null if none.
    public OverriddenMethodNode getOverriddenMethod() {
        return overrides;
    }

    /// Returns the return type of this method.
    /// @return the TypeNode representing the return type.
    public TypeNode getReturnType() {
        return returnType;
    }

    /// Adds a parameter to this method.
    /// @param param a ParamNode representing the parameter to add.
    public void addParam(ParamNode param) {
        params.add(param);
    }

    /// Returns the list of parameters of this method.
    /// @return List of ParamNode objects representing the method parameters.
    public List<ParamNode> getParams() {
        return params;
    }

    /// Adds an exception type that this method declares it throws.
    /// @param name the fully qualified name of the exception type.
    public void addThrownType(String name) {
        thrownTypes.add(name);
    }

    /// Returns the list of exception types declared by this method.
    /// @return List of exception type names as Strings.
    public List<String> getThrownTypes() {
        return thrownTypes;
    }

    /// Sets the owning type (class/interface) of this method.
    /// @param owner the TypeNode representing the owner.
    public void setOwner(TypeNode owner) {
        this.owner = owner;
    }

    /// Returns the owning type of this method.
    /// @return the TypeNode representing the owner.
    public TypeNode getOwner() {
        return owner;
    }

    /// Sets the interface or specification name this method is specified by.
    /// @param interfaceName the name of the specifying interface or specification.
    public void setSpecifiedBy(String interfaceName) {
        this.specifiedBy = interfaceName;
    }

    /// Returns the name of the interface or specification this method is specified by.
    /// @return the specifying interface or specification name.
    public String getSpecifiedBy() {
        return specifiedBy;
    }

    /// Sets the description of the method's return value.
    /// @param text a Text object describing the return value.
    public void setReturnDescription(Text text) {
        returnDescription = text;
    }

    /// Returns the description of the method's return value.
    /// @return a Text object containing the return description.
    public Text getReturnDescription() {
        return returnDescription;
    }

    /// Computes and returns the method signature string, including return type, name, and parameters.
    /// Example format: "java.lang.String methodName(int, java.util.List)"
    /// @return the method signature as a String.
    public String signature(){
        StringBuilder sb = new StringBuilder();
        sb.append(returnType.qualifiedName);
        sb.append(" ");
        sb.append(simpleName);
        sb.append("(");
        int paramCount = 0;
        for (ParamNode param : params) {
            if (paramCount++ > 0) sb.append(", ");
            String typeName = param.getType().qualifiedName;
            if (typeName == null) typeName = param.getType().simpleName;
            sb.append(typeName);
        }
        sb.append(")");
        return sb.toString();
    }

    /// Sets the simple name of the method.
    /// @param name the simple name to set.
    public void setSimpleName(String name) {
        simpleName = name;
    }

    /// Returns the simple name of the method.
    /// @return the simple name as a String.
    public String getSimpleName() {
        return simpleName;
    }
}