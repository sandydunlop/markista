package io.github.sandydunlop.markista.model;

public class ClassNode extends TypeNode {
    public ClassNode(String qualifiedName, String simpleName, String packageName) {
        super(qualifiedName, simpleName, packageName);
    }

    public FieldNode getField(String fieldName) {
        //TODO: Make this efficient
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
}
