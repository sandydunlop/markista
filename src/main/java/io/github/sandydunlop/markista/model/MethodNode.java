package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.type.TypeMirror;

public class MethodNode extends Node {
    private Text returnDescription = Text.empty();
    private String specifiedBy = "";
    private TypeNode returnType = null;
    private OverriddenMethodNode overrides = null;
    private List <ParamNode> params = new ArrayList<>();
    private List<? extends TypeMirror> thrownTypes = new ArrayList<>();
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
    
    public void setReturnType(TypeNode returnType) {
        this.returnType = returnType;
    }

    public TypeNode getReturnType() {
        return returnType;
    }

    public void setParams(List<ParamNode> params) {
        this.params = params;
    }

    public List<ParamNode> getParams() {
        return params;
    }

    public void setThrownTypes(List<? extends TypeMirror> thrownTypes) {
        this.thrownTypes = thrownTypes;
    }

    public List<? extends TypeMirror> getThrownTypes() {
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
        String sig = returnType.qualifiedName + " ";
        sig += simpleName + "(";
        int paramCount = 0;
        for (ParamNode param : params) {
            if (paramCount++ > 0) sig += ", ";
            String typeName = param.getType().qualifiedName;
            if (typeName == null) typeName = param.getType().simpleName;
            sig+=typeName;
        }
        sig += ")";
        return sig;
    }
    public String fullSignature() {
        String sig = getModifiersString();
        sig += returnType.simpleName + " ";
        sig += simpleName + "(" + paramsString() + ")";
        return sig;
    }
    public String paramsString(){
        String str = "";
        int paramCount = 0;
        for (ParamNode param : params) {
            if (paramCount++ > 0) str += ", ";
            String typeName = param.getType().simpleName;
            if (typeName == null) typeName = param.getType().simpleName;
            str+=typeName + " " + param.simpleName; 
        }
        return str;
    }

    public void setSimpleName(String name) {
        simpleName = name;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public void setQualifiedName(String name) {
        qualifiedName = name;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }
}
