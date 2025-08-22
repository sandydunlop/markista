package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

/// Represents a type in the API model, including its kind (class, interface, enum, annotation),
/// supertypes, implemented interfaces, constructors, methods, fields, ownership, and relevant metadata.
public class TypeNode extends PackageOrTypeNode implements TypeView {

    protected String sourcePath;

    /// The owner of this type, usually another type or module.
    private String owner = "";

    private Reference enclosingClassRef = null;

    /// List of qualified names of interfaces implemented by this type.
    private List<Reference> implementedInterfaces = new ArrayList<>();

    /// List of qualified names of this type's supertypes.
    private List<Pair<Reference, Text>> supertypes = new ArrayList<>();

    /// String representation of array brackets if this type is an array (e.g., `[]`).
    private String arrayBrackets = "";

    /// List of constructor methods belonging to this type.
    private final List<MethodNode> constructors = new ArrayList<>();

    /// List of methods belonging to this type.
    private final List<MethodNode> methods = new ArrayList<>();

    /// List of fields belonging to this type.
    private final List<FieldNode> fields = new ArrayList<>();

    /// Has the `@Documented` annotation applied
    private boolean hasDocumentedAnnotation = false;

    /// Constructs a TypeNode with the specified qualified name, simple name, and package.
    /// @param qualifiedName the fully qualified name of this type.
    /// @param simpleName the simple name of this type.
    /// @param packageName the name of the package that contains this type.
    public TypeNode(String qualifiedName, String simpleName, String packageName) {
        this.qualifiedName = qualifiedName;
        this.simpleName = simpleName;
        this.packageName = packageName;
    }

    @Override
    public boolean isClass() { return kind == NodeKind.CLASS; }

    @Override
    public boolean isInterface() { return kind == NodeKind.INTERFACE; }
    
    @Override
    public boolean isEnum() { return kind == NodeKind.ENUM; }
    
    @Override
    public boolean isRecord() { return kind == NodeKind.RECORD; }
    
    @Override
    public boolean isAnnotation() { return kind == NodeKind.ANNOTATION; }

    @Override
    public String getKindName() { return kind.toString(); }

    public void setSourcePath(String path) {
        this.sourcePath = path;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    /// Sets the array brackets representation for this type.
    /// @param brackets string representing array dimension brackets (e.g., `[]`).
    public void setArrayBrackets(String brackets) {
        this.arrayBrackets = brackets;
    }

    /// Returns the array brackets representation for this type.
    /// @return string representing array dimension brackets.
    public String getArrayBrackets() {
        return arrayBrackets;
    }

    /// Sets the list of implemented interfaces by qualified names.
    /// @param implementedInterfaces list of qualified interface names.
    public void setImplementedInterfaces(List<Reference> implementedInterfaces) {
        this.implementedInterfaces = implementedInterfaces;
    }

    /// Returns the list of supertypes by qualified names.
    /// @return list of qualified supertype names.
    public List<Pair<Reference, Text>> getSupertypes() {
        return supertypes;
    }

    /// Returns the list of implemented interfaces by qualified names.
    /// @return list of qualified interface names.
    public List<Reference> getImplementedInterfaces() {
        return implementedInterfaces;
    }

    /// Sets the owner of this type.
    /// @param owner the TypeOwner that owns this type.
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /// Returns the owner of this type.
    /// @return the TypeOwner that owns this type.
    public String getOwner() {
        return owner;
    }

    /// Returns the package name for this type.
    /// @return qualified package name, or null if no package node.
    public String getPackageName() {
        return packageName;
    }

    /// Adds a method to this type.
    /// @param method the MethodNode to add.
    public void addMethod(MethodNode method) {
        methods.add(method);
    }

    /// Returns the list of methods of this type.
    /// @return list of MethodNode instances.
    public List<MethodNode> getMethods() {
        return methods;
    }

    /// Adds a constructor method to this type.
    /// @param constructor the MethodNode constructor to add.
    public void addConstructor(MethodNode constructor) {
        constructors.add(constructor);
    }

    /// Returns the list of constructors of this type.
    /// @return list of MethodNode constructors.
    public List<MethodNode> getConstructors() {
        return constructors;
    }

    /// Adds a field to this type.
    /// @param field the FieldNode to add.
    public void addField(FieldNode field) {
        fields.add(field);
    }

    /// Returns the list of fields of this type.
    /// @return list of FieldNode instances.
    public List<FieldNode> getFields() {
        return fields;
    }

    /// Sets a flag indicating if this type as having a `@Documented` meta-annotation
    /// @param b If true, this type is marked as having a `@Documented` meta-annotation.
    /// If false, it is marked as not having the met-annotation.
    public void setHasDocumentedAnnotation(boolean b) {
        this.hasDocumentedAnnotation = b;
    }

    /// Does this type have a `@Documented` meta-annotation?
    /// @return True if it has a `@Documented` meta-annotation
    public boolean hasDocumentedAnnotation() {
        return hasDocumentedAnnotation;
    }

    public void setEnclosingClassRef(Reference ref) {
        enclosingClassRef = ref;
    }

    public Reference getEnclosingClassRef() {
        return enclosingClassRef;
    }

    /// Retrieves a field by its simple name.
    /// @param fieldName the simple name of the field.
    /// @return the FieldNode if found, otherwise null.
    public FieldNode getField(String fieldName) {
        for (FieldNode fieldNode : fields) {
            if (fieldNode.getSimpleName().equals(fieldName)){
                return fieldNode;
            }
        }
        return null;
    }

    /// Retrieves a method matching the signature of a given MethodNode.
    /// @param method the MethodNode whose signature to match.
    /// @return the matching MethodNode if found, otherwise null.
    public MethodNode getMethod(MethodNode method) {
        String sig = method.signature();
        for (MethodNode existingMethod : methods) {
            if (existingMethod.signature().equals(sig)){
                return existingMethod;
            }
        }
        return null;
    }

    /// Retrieves a constructor matching the signature of a given MethodNode.
    /// @param method the MethodNode whose signature to match.
    /// @return the matching constructor MethodNode if found, otherwise null.
    public MethodNode getConstructor(MethodNode method) {
        String sig = method.signature();
        for (MethodNode existingMethod : constructors) {
            if (existingMethod.signature().equals(sig)){
                return existingMethod;
            }
        }
        return null;
    }

    /// {@inheritDoc}
    /// @return A string containing sorted modifiers separated by spaces.
    @Override
    public String getModifiersString() {
        StringBuilder mods = new StringBuilder();
        List<Modifier> modifierList = ModifierSorter.sortModifiers(getModifiers());
        for (Modifier mod : modifierList) {
            if ((kind != NodeKind.ANNOTATION && kind != NodeKind.INTERFACE) || mod != Modifier.ABSTRACT) {
                mods.append(mod.toString()).append(" ");
            }
        }
        return mods.toString();
    }

    /// Sorts the nodes owned by this instance into alphabetical order.
    @Override
    public void sort() {
        super.sort();
        fields.sort((a, b) -> a.getSimpleName().compareTo(b.getSimpleName()));
        methods.sort((a, b) -> a.getSimpleName().compareTo(b.getSimpleName()));
    }
}