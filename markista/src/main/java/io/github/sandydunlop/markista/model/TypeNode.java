package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;


public class TypeNode extends AbstractTypeOwner implements PackageMember {
    private TypeOwner owner = null;
    private List<String> implementedInterfaces = new ArrayList<>();
    private List<String> supertypes = new ArrayList<>();
    private String arrayBrackets = "";
    private List<MethodNode> constructors = new ArrayList<>();
    private List<MethodNode> methods = new ArrayList<>();
    private List<FieldNode> fields = new ArrayList<>();
    protected Kind kind = Kind.NONE;
    
    public TypeNode(String qualifiedName, String simpleName, PackageNode packageNode) {
        this.qualifiedName = qualifiedName;
        this.simpleName = simpleName;
        this.packageNode = packageNode;
    }

    public void setArrayBrackets(String brackets) {
        this.arrayBrackets = brackets;
    }

    public String getArrayBrackets() {
        return arrayBrackets;
    }

    public void setImplementedInterfaces(List<String> implementedInterfaces) {
        this.implementedInterfaces = implementedInterfaces;
    }

    public void setSupertypes(List<String> supertypes) {
        this.supertypes = supertypes;
    }

    public List<String> getSupertypes() {
        return supertypes;
    }

    public List<String> getImplementedInterfaces() {
        return implementedInterfaces;
    }

    public void setOwner(TypeOwner owner) {
        this.owner = owner;
    }

    public TypeOwner getOwner() {
        return owner;
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

    public String getPackageName() {
        if (packageNode == null) return null;
        return packageNode.qualifiedName;
    }

    public PackageNode getPackage() {
        return packageNode;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }

    public void addMethod(MethodNode method) {
        methods.add(method);
    }

    public List<MethodNode> getMethods() {
        return methods;
    }

    public void addConstructor(MethodNode constructor) {
        constructors.add(constructor);
    }

    public List<MethodNode> getConstructors() {
        return constructors;
    }

    public void addField(FieldNode field) {
        fields.add(field);
    }

    public List<FieldNode> getFields() {
        return fields;
    }

    public FieldNode getField(String fieldName) {
        for (FieldNode fieldDoc : fields) {
            if (fieldDoc.simpleName.equals(fieldName)){
                return fieldDoc;
            }
        }
        return null;
    }

    public MethodNode getMethod(MethodNode method) {
        String sig = method.signature();
        for (MethodNode existingMethod : methods) {
            if (existingMethod.signature().equals(sig)){
                return existingMethod;
            }
        }
        return null;
    }
    
    public MethodNode getConstructor(MethodNode method) {
        String sig = method.signature();
        for (MethodNode existingMethod : constructors) {
            if (existingMethod.signature().equals(sig)){
                return existingMethod;
            }
        }
        return null;
    }

    public String getName() {
        return simpleName;
    }

    public Text getDescription() {
        return firstSentence;
    }

    public enum Kind {
        NONE ("None"),
        CLASS ("Class"),
        INTERFACE ("Interface"),
        ENUM ("Enum"),
        ANNOTATION ("Annotation");

        private final String name;       

        private Kind(String s) {
            name = s;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}
