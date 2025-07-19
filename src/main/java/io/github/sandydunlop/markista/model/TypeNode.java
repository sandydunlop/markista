package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;


public class TypeNode extends Node {
    public List<String> implementedInterfaces = new ArrayList<>();
    public List<String> supertypes = new ArrayList<>();
    public String fullDescription = "";
    public String arrayBrackets = "";
    public List<ClassNode> classes = new ArrayList<>();
    public List<InterfaceNode> interfaces = new ArrayList<>();
    public List<EnumNode> enumClasses = new ArrayList<>();
    public List<ExceptionNode> exceptionClasses = new ArrayList<>();
    public List<AnnotationNode> annotationClasses = new ArrayList<>();
    public List<MethodNode> constructors = new ArrayList<>();
    public List<MethodNode> methods = new ArrayList<>();
    public List<FieldNode> fields = new ArrayList<>();
    
    public TypeNode(String qualifiedName, String simpleName, String packageName) {
        this.qualifiedName = qualifiedName;
        this.simpleName = simpleName;
        this.packageName = packageName;
    }

    public void sort() {
        Collections.sort(implementedInterfaces);
        Collections.sort(classes, (o1, o2) -> o2.simpleName.compareTo(o1.simpleName));
        Collections.sort(interfaces, (o1, o2) -> o2.simpleName.compareTo(o1.simpleName));
        Collections.sort(enumClasses, (o1, o2) -> o2.simpleName.compareTo(o1.simpleName));
        Collections.sort(exceptionClasses, (o1, o2) -> o2.simpleName.compareTo(o1.simpleName));
        Collections.sort(annotationClasses, (o1, o2) -> o2.simpleName.compareTo(o1.simpleName));
        Collections.sort(constructors, (o1, o2) -> o2.simpleName.compareTo(o1.simpleName));
        Collections.sort(methods, (o1, o2) -> o2.simpleName.compareTo(o1.simpleName));
        Collections.sort(fields, (o1, o2) -> o2.simpleName.compareTo(o1.simpleName));
    }
}
