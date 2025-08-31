package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Represents a type in the API model, including its kind (class, interface, enum, annotation),
/// supertypes, implemented interfaces, constructors, methods, fields, ownership, and relevant metadata.
public class TypeNode extends AbstractMember {

    protected String sourcePath;

    /// The owner of this type — a type or package.
    private String ownerName = "";

    private Link enclosingClassRef = null;

    // store children by the interface type (no concrete TypeNode mention)
    private final List<TypeNode> types = new ArrayList<>();

    /// List of references to interfaces implemented by this type and text containing links.
    private List<TypeReference> implementedInterfaces = new ArrayList<>();

    /// List of references to this type's supertypes and text containing links.
    private List<TypeReference> supertypes = new ArrayList<>();

    /// List of references to this type's subtypes and text containing links.
    private List<TypeReference> subtypes = new ArrayList<>();

    private HashMap<TypeReference,List<Link>> inheritedMethods = new HashMap<>();

    /// List of constructor methods belonging to this type.
    private final List<MethodNode> constructors = new ArrayList<>();

    /// List of methods belonging to this type.
    private final List<MethodNode> methods = new ArrayList<>();

    /// List of fields belonging to this type.
    private final List<FieldNode> fields = new ArrayList<>();

    /// Has the `@Documented` annotation applied
    private boolean hasDocumentedAnnotation = false;

    /// Constructs a TypeNode with the specified simple name and package.
    /// @param simpleName the simple name of this type.
    /// @param packageName the name of the package that contains this type.
    public TypeNode(String simpleName, String packageName) {
        if (packageName != null && !packageName.isEmpty()) {
            qualifiedName = packageName + "." + simpleName;
        } else {
            qualifiedName = simpleName;
        }
        this.simpleName = simpleName;
        this.packageName = packageName;
    }

    public boolean isClass() { return kind == Node.Kind.CLASS; }

    public boolean isInterface() { return kind == Node.Kind.INTERFACE; }

    public boolean isEnum() { return kind == Node.Kind.ENUM; }

    public boolean isRecord() { return kind == Node.Kind.RECORD; }

    public boolean isAnnotation() { return kind == Node.Kind.ANNOTATION; }

    public String getKindName() { return kind.toString(); }

    public void setSourcePath(String path) {
        this.sourcePath = path;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    /// Returns the list of implemented interfaces by qualified names.
    /// @return list of qualified interface names.
    public List<TypeReference> getImplementedInterfaces() {
        return implementedInterfaces;
    }

    /// Returns the list of supertype references and text.
    /// @return list of supertype references and text.
    public List<TypeReference> getSupertypes() {
        return supertypes;
    }

    /// Returns the list of subtype references and text.
    /// @return list of subtype references and text.
    public List<TypeReference> getSubtypes() {
        return subtypes;
    }

    /// Returns the list of inherited methods organized by the type they are defined in.
    /// @return HashMap of types containing inherited methods to inherited methods.
    public Map<TypeReference,List<Link>> getInheritedMethods() {
        return inheritedMethods;
    }

    /// Sets the owner of this type.
    /// @param owner the TypeOwner that owns this type.
    public void setOwnerName(String owner) {
        this.ownerName = owner;
    }

    /// Returns the owner of this type.
    /// @return the TypeOwner that owns this type.
    public String getOwnerName() {
        return ownerName;
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

    public void setEnclosingClassRef(Link ref) {
        enclosingClassRef = ref;
    }

    public Link getEnclosingClassRef() {
        return enclosingClassRef;
    }

    public void addType(TypeNode typeNode) {
        types.add(typeNode);
    }

    /// Gets the list of types *owned* by this instance.
    public List<TypeNode> getTypes() {
        return List.copyOf(types);
    }

    /// Gets the list of classes *owned* by this instance.
    public List<TypeNode> getClasses() {
        List<TypeNode> out = new ArrayList<>();
        for (TypeNode t : types)
            if (t.isClass())
                out.add(t);
        return out;
    }

    /// Gets the list of interfaces *owned* by this instance.
    public List<TypeNode> getInterfaces() {
        List<TypeNode> out = new ArrayList<>();
        for (TypeNode t : types)
            if (t.isInterface())
                out.add(t);
        return out;
    }

    /// Gets the list of enums *owned* by this instance.
    public List<TypeNode> getEnums() {
        List<TypeNode> out = new ArrayList<>();
        for (TypeNode t : types)
            if (t.isEnum())
                out.add(t);
        return out;
    }

    /// Gets the list of records *owned* by this instance.
    public List<TypeNode> getRecords() {
        List<TypeNode> out = new ArrayList<>();
        for (TypeNode t : types)
            if (t.isRecord())
                out.add(t);
        return out;
    }

    /// Gets the list of annotations *owned* by this instance.
    public List<TypeNode> getAnnotations() {
        List<TypeNode> out = new ArrayList<>();
        for (TypeNode t : types)
            if (t.isAnnotation())
                out.add(t);
        return out;
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
        String sig = method.simplifiedSignature();
        for (MethodNode existingMethod : methods) {
            if (existingMethod.simplifiedSignature().equals(sig)){
                return existingMethod;
            }
        }
        return null;
    }

    /// Retrieves a constructor matching the signature of a given MethodNode.
    /// @param method the MethodNode whose signature to match.
    /// @return the matching constructor MethodNode if found, otherwise null.
    public MethodNode getConstructor(MethodNode method) {
        String sig = method.simplifiedSignature();
        for (MethodNode existingMethod : constructors) {
            if (existingMethod.simplifiedSignature().equals(sig)){
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
            if ((kind != Node.Kind.ANNOTATION && kind != Node.Kind.INTERFACE) || mod != Modifier.ABSTRACT) {
                mods.append(mod.toString()).append(" ");
            }
        }
        return mods.toString();
    }

    /// Sorts the nodes owned by this instance into alphabetical order.
    public void sort() {
        types.sort((a, b) -> a.getSimpleName().compareTo(b.getSimpleName()));
        fields.sort((a, b) -> a.getSimpleName().compareTo(b.getSimpleName()));
        methods.sort((a, b) -> a.getSimpleName().compareTo(b.getSimpleName()));
    }
}