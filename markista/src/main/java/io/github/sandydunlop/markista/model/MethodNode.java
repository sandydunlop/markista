package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

/// Contains information about a method being documented
public class MethodNode extends AbstractMember {
    /// Description of the method's return value.
    private Text returnDescription = Text.empty();

    /// Name of the interface or specification this method is specified by.
    private Link specifiedBy;

    /// The return type of this method.
    private final String returnTypeName;

    /// Information about the method that this method overrides, if any.
    private MethodReference baseMethod = null;

    /// List of parameters for this method.
    private final List<ParamNode> params = new ArrayList<>();

    /// List of exception types that this method declares it can throw.
    private final List<Link> thrownTypes = new ArrayList<>();

    /// The type (class/interface) that owns this method.
    private String ownerName = null;

    private Text returnTypeText = Text.empty();

    /// Constructs a MethodNode with the specified return type and method name.
    /// @param returnType the return type of the method.
    /// @param name       the simple name of the method.
    public MethodNode(String returnType, String name) {
        this.returnTypeName = returnType;
        this.simpleName = name;
    }

    /// Sets the base method information that this method overrides.
    /// @param baseMethod a reference to the base method and the text representing it
    public void setBaseMethod(MethodReference baseMethod) {
        this.baseMethod = baseMethod;
    }

    /// Returns the base method information, if any.
    /// @return the a reference to the base method and the text representing it, or null if none.
    public MethodReference getBaseMethod() {
        return baseMethod;
    }

    /// Returns the return type of this method.
    /// @return the TypeNode representing the return type.
    public String getReturnTypeName() {
        return returnTypeName;
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
    public void addThrownType(Link name) {
        thrownTypes.add(name);
    }

    /// Returns the list of exception types declared by this method.
    /// @return List of exception type names as Strings.
    public List<Link> getThrownTypes() {
        return thrownTypes;
    }

    /// Sets the owning type (class/interface) of this method.
    /// @param owner the name of the TypeNode representing the owner.
    public void setOwnerName(String owner) {
        this.ownerName = owner;
    }

    /// Returns the owning type of this method.
    /// @return the name of the TypeNode representing the owner.
    public String getOwnerName() {
        return ownerName;
    }

    /// Sets the interface or specification name this method is specified by.
    /// @param interfaceName the name of the specifying interface or specification.
    public void setSpecifiedBy(Link interfaceName) {
        this.specifiedBy = interfaceName;
    }

    /// Returns the name of the interface or specification this method is specified by.
    /// @return the specifying interface or specification name.
    public Link getSpecifiedBy() {
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

    /// Returns the type Text of this parameter.
    /// @return The Text representing the parameter's type.
    public Text getReturnTypeText() {
        return returnTypeText;
    }

    /// Sets the type Text of this parameter.
    /// @param text The Text to set as this parameter's type Text.
    public void setReturnTypeText(Text text) {
        this.returnTypeText = text;
    }

    /// Computes and returns the method signature string, including return type, name, and parameters.
    /// Example format: "java.lang.String methodName(int,java.util.List)"
    /// @return the method signature as a String.
    public String signature(){
        StringBuilder sb = new StringBuilder();
        sb.append(simpleName);
        sb.append("(");
        int paramCount = 0;
        for (ParamNode param : params) {
            if (paramCount++ > 0) sb.append(",");
            String typeName = param.getTypeName();
            if (typeName == null) typeName = param.getTypeName();
            sb.append(typeName);
        }
        sb.append(")");
        return sb.toString();
    }
}