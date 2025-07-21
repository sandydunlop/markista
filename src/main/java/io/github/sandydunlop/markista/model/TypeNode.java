package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;


public class TypeNode extends AbstractTypeOwner implements PackageMember {
    public TypeOwner owner = null;
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
    protected Kind kind = Kind.NONE;
    
    public TypeNode(String qualifiedName, String simpleName, PackageNode packageNode) {
        this.qualifiedName = qualifiedName;
        this.simpleName = simpleName;
        this.packageNode = packageNode;
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

    public void sort() {
        Collections.sort(implementedInterfaces);
        Collections.sort(classes, (o1, o2) -> o1.simpleName.compareTo(o2.simpleName));
        Collections.sort(interfaces, (o1, o2) -> o1.simpleName.compareTo(o2.simpleName));
        Collections.sort(enumClasses, (o1, o2) -> o1.simpleName.compareTo(o2.simpleName));
        Collections.sort(exceptionClasses, (o1, o2) -> o1.simpleName.compareTo(o2.simpleName));
        Collections.sort(annotationClasses, (o1, o2) -> o1.simpleName.compareTo(o2.simpleName));
        Collections.sort(constructors, (o1, o2) -> o1.simpleName.compareTo(o2.simpleName));
        Collections.sort(methods, (o1, o2) -> o1.simpleName.compareTo(o2.simpleName));
        Collections.sort(fields, (o1, o2) -> o1.simpleName.compareTo(o2.simpleName));
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
