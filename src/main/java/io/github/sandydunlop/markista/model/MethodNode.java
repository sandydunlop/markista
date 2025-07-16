package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import javax.lang.model.type.TypeMirror;

public class MethodNode extends Node {
    public String returnDescription = "";
    public String fullDescription = "";
    public TypeNode returnType = null;
    public List <ParamNode> params = new ArrayList<>();
    public List<? extends TypeMirror> thrownTypes = new ArrayList<>();
    public MethodNode(TypeNode returnType, String name) {
        this.returnType = returnType;
        this.simpleName = name;
    }
    public String signature(){
        String sig = returnType.qualifiedName + " ";
        sig += simpleName + "(";
        int paramCount = 0;
        for (ParamNode param : params) {
            if (paramCount++ > 0) sig += ", ";
            String typeName = param.type.qualifiedName;
            if (typeName == null) typeName = param.type.simpleName;
            sig+=typeName;
        }
        sig += ")";
        return sig;
    }
    public String fullSignature() {
        String sig = getModifiers();
        sig += returnType.simpleName + " ";
        sig += simpleName + "(" + paramsString() + ")";
        return sig;
    }
    public String paramsString(){
        String str = "";
        int paramCount = 0;
        for (ParamNode param : params) {
            if (paramCount++ > 0) str += ", ";
            String typeName = param.type.simpleName;
            if (typeName == null) typeName = param.type.simpleName;
            str+=typeName + " " + param.simpleName; 
        }
        return str;
    }
}
