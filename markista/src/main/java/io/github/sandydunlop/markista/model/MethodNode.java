package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

public class MethodNode extends Node {
    private Text returnDescription = Text.empty();
    private String specifiedBy = "";
    private TypeNode returnType = null;
    private OverriddenMethodNode overrides = null;
    private List<ParamNode> params = new ArrayList<>();
    private List<String> thrownTypes = new ArrayList<>();
    private TypeNode owner = null;

    public MethodNode(TypeNode returnType, String name) {
        this.returnType = returnType;
        this.simpleName = name;
    }

    public void setOverriddenMethod(OverriddenMethodNode overrides) {
        this.overrides = overrides;
    }

    public OverriddenMethodNode getOverriddenMethod() {
        return overrides;
    }
    
    public TypeNode getReturnType() {
        return returnType;
    }

    public void addParam(ParamNode param) {
        params.add(param);
    }

    public List<ParamNode> getParams() {
        return params;
    }

    public void addThrownType(String name) {
        thrownTypes.add(name);
    }

    public List<String> getThrownTypes() {
        return thrownTypes;
    }

    public void setOwner(TypeNode owner) {
        this.owner = owner;
    }

    public TypeNode getOwner() {
        return owner;
    }

    public void setSpecifiedBy(String interfaceName) {
        specifiedBy = interfaceName;
    }

    public String getSpecifiedBy() {
        return specifiedBy;
    }

    public void setReturnDescription(Text text) {
        returnDescription = text;
    }

    public Text getReturnDescription() {
        return returnDescription;
    }

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

    public void setSimpleName(String name) {
        simpleName = name;
    }

    public String getSimpleName() {
        return simpleName;
    }
}
