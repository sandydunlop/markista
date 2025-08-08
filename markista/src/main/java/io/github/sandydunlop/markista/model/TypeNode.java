package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;


/// Represents a type in the API model, including its kind (class, interface, enum, annotation),
/// supertypes, implemented interfaces, constructors, methods, fields, ownership, and relevant metadata.
public class TypeNode extends AbstractTypeOwner implements PackageMember {
    /// The canonical form of the type's name
    protected String qualifiedName;

    /// The owner of this type, usually another type or module.
    private TypeOwner owner = null;

    /// List of annotations applied to this type.
    private final List<AppliedAnnotationNode> appliedAnnotations = new ArrayList<>();

    /// List of qualified names of interfaces implemented by this type.
    private List<String> implementedInterfaces = new ArrayList<>();

    /// List of qualified names of this type's supertypes.
    private List<String> supertypes = new ArrayList<>();

    /// String representation of array brackets if this type is an array (e.g., `[]`).
    private String arrayBrackets = "";

    /// List of constructor methods belonging to this type.
    private final List<MethodNode> constructors = new ArrayList<>();

    /// List of methods belonging to this type.
    private final List<MethodNode> methods = new ArrayList<>();

    /// List of fields belonging to this type.
    private final List<FieldNode> fields = new ArrayList<>();

    /// The kind of this type (e.g., class, interface, enum, annotation).
    protected Kind kind = Kind.NONE;

    /// Has the `@Documented` annotation applied
    private boolean hasDocumentedAnnotation = false;


    /// Constructs a TypeNode with the specified qualified name, simple name, and package.
    /// @param qualifiedName the fully qualified name of this type.
    /// @param simpleName the simple name of this type.
    /// @param packageNode the PackageNode that contains this type.
    public TypeNode(String qualifiedName, String simpleName, PackageNode packageNode) {
        this.qualifiedName = qualifiedName;
        this.simpleName = simpleName;
        this.packageNode = packageNode;
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

    /// Adds an applied annotation to this type
    /// @param annotation the annotation
    public void addAppliedAnnotation(AppliedAnnotationNode annotation) {
        appliedAnnotations.add(annotation);
    }

    /// Returns the list of annotations applied to this type.
    /// @return list of applied annotations
    public List<AppliedAnnotationNode> getAppliedAnnotations() {
        return appliedAnnotations;
    }

    /// Sets the list of implemented interfaces by qualified names.
    /// @param implementedInterfaces list of qualified interface names.
    public void setImplementedInterfaces(List<String> implementedInterfaces) {
        this.implementedInterfaces = implementedInterfaces;
    }

    /// Sets the list of supertypes by qualified names.
    /// @param supertypes list of qualified supertype names.
    public void setSupertypes(List<String> supertypes) {
        this.supertypes = supertypes;
    }

    /// Returns the list of supertypes by qualified names.
    /// @return list of qualified supertype names.
    public List<String> getSupertypes() {
        return supertypes;
    }

    /// Returns the list of implemented interfaces by qualified names.
    /// @return list of qualified interface names.
    public List<String> getImplementedInterfaces() {
        return implementedInterfaces;
    }

    /// Sets the owner of this type.
    /// @param owner the TypeOwner that owns this type.
    public void setOwner(TypeOwner owner) {
        this.owner = owner;
    }

    /// Returns the owner of this type.
    /// @return the TypeOwner that owns this type.
    public TypeOwner getOwner() {
        return owner;
    }

    /// Sets the simple name of this type.
    /// @param name the simple name to set.
    public void setSimpleName(String name) {
        simpleName = name;
    }

    /// Returns the simple name of this type.
    /// @return the simple name.
    public String getSimpleName() {
        return simpleName;
    }

    /// Sets the qualified name of this type.
    /// @param name the qualified name to set.
    public void setQualifiedName(String name) {
        qualifiedName = name;
    }

    /// Returns the qualified name of this type.
    /// @return the qualified name.
    public String getQualifiedName() {
        return qualifiedName;
    }

    /// Returns the package name for this type.
    /// @return qualified package name, or null if no package node.
    public String getPackageName() {
        if (packageNode == null) return null;
        return packageNode.getName();
    }

    /// Returns the package node this type belongs to.
    /// @return the PackageNode instance.
    public PackageNode getPackage() {
        return packageNode;
    }

    /// Sets the kind (class, interface, enum, annotation) of this type.
    /// @param kind the Kind enum value.
    public void setKind(Kind kind) {
        this.kind = kind;
    }

    /// Returns the kind of this type.
    /// @return the Kind enum value.
    public Kind getKind() {
        return kind;
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

    /// Retrieves a field by its simple name.
    /// @param fieldName the simple name of the field.
    /// @return the FieldNode if found, otherwise null.
    public FieldNode getField(String fieldName) {
        for (FieldNode fieldDoc : fields) {
            if (fieldDoc.simpleName.equals(fieldName)){
                return fieldDoc;
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

    /// Returns the name of the type.
    /// @return the simple name.
    public String getName() {
        return simpleName;
    }

    /// Returns the description of this type.
    /// @return a Text object containing the first sentence or summary.
    public Text getDescription() {
        return firstSentence;
    }

    /// Returns a string representation of modifiers.
    /// The modifiers are sorted according to a predefined order.
    /// @return A string containing sorted modifiers separated by spaces.
    @Override
    public String getModifiersString() {
        StringBuilder mods = new StringBuilder();
        List<Modifier> modifierList = ModifierSorter.sortModifiers(getModifiers());
        for (Modifier mod : modifierList) {
            if (kind != Kind.ANNOTATION || mod != Modifier.ABSTRACT) {
                mods.append(mod.toString()).append(" ");
            }
        }
        return mods.toString();
    }

    /// Enumeration representing kinds of types: None, Class, Interface, Enum, Annotation.
    public enum Kind {
        /// No type has been set
        NONE ("None"),

        /// A class, including abstract class and exception class
        CLASS ("Class"),

        /// An interface
        INTERFACE ("Interface"),

        /// An enum
        ENUM ("Enum"),

        /// An annotation
        ANNOTATION ("Annotation Type");

        /// The display name for the kind.
        private final String name;

        /// Constructor assigning the display name.
        Kind(String s) {
            name = s;
        }

        /// Returns the display name of the kind.
        @Override
        public String toString() {
            return this.name;
        }
    }
}