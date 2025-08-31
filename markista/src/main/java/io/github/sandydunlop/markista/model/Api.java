package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

/// Represents the API being documented, encapsulating its modules and packages.
/// Provides methods to add and retrieve modules, packages, and types, as well as sorting them.
public class Api extends Node {
    /// The name of the API
    private String name = "API";

    /// List of modules included in the API.
    private final List<ModuleNode> modules = new ArrayList<>();

    /// List of packages included in the API.
    private final List<PackageNode> packages = new ArrayList<>();

    // store children by the interface type (no concrete TypeNode mention)
    private final List<TypeNode> types = new ArrayList<>();

    private List<MethodNode> methods = new ArrayList<>();

    /// Represents the unnamed module in the API.
    private final ModuleNode unnamedModule = new ModuleNode("");

    /// List of applied annotations.
    private final List<AppliedAnnotationNode> appliedAnnotations = new ArrayList<>();

    /// List of links.
    private final List<Link> links = new ArrayList<>();

    /// Constructs an empty Api instance with the given name.
    /// @param name The name of the API
    public Api(String name) {
        this.name = name;
    }

    /// Returns the name of the API.
    /// @return The name of the API
    public String getName() {
        return name;
    }

    /// Adds a module to the API.
    /// @param node the ModuleNode instance to add.
    public void addModule(ModuleNode node) {
        modules.add(node);
    }

    /// Returns the list of modules in the API.
    /// @return List of ModuleNode objects representing the modules.
    public List<ModuleNode> getModules() {
        return modules;
    }

    /// Adds a method to the API.
    /// @param node the MethodNode instance to add.
    public void addMethod(MethodNode node) {
        methods.add(node);
    }

    /// Returns the list of methods in the API.
    /// @return List of MethodNode objects representing the methods.
    public List<MethodNode> getMethods() {
        return methods;
    }

    /// Retrieves a module matching the specified qualified name.
    /// @param qualifiedName the fully qualified name of the module.
    /// @return the matching ModuleNode if found, or null otherwise.
    public ModuleNode getModuleNode(String qualifiedName) {
        for (ModuleNode moduleNode : modules) {
            if (moduleNode.getName().equals(qualifiedName)){
                return moduleNode;
            }
        }
        return null;
    }

    /// Returns the unnamed module of the API.
    /// @return the unnamed ModuleNode.
    public ModuleNode getUnnamedModuleNode() {
        return unnamedModule;
    }

    /// Adds a package to the API.
    /// @param node the PackageNode instance to add.
    public void addPackage(PackageNode node) {
        packages.add(node);
    }

    /// Returns the list of packages in the API.
    /// @return List of PackageNode objects representing the packages.
    public List<PackageNode> getPackages() {
        return packages;
    }

    /// Adds an applied annotation
    /// @param annotation the annotation
    public void addAppliedAnnotation(AppliedAnnotationNode annotation) {
        appliedAnnotations.add(annotation);
    }

    /// Returns the list of applied annotations
    /// @return list of applied annotations
    public List<AppliedAnnotationNode> getAppliedAnnotations() {
        return appliedAnnotations;
    }

    /// Adds a link
    /// @param link the link to add
    public void addLink(Link link) {
        links.add(link);
    }

    /// Returns the list of link.
    /// @return list of links
    public List<Link> getLinks() {
        return links;
    }

    public void addType(TypeNode typeNode) {
        types.add(typeNode);
    }

    /// Gets the list of types *owned* by this instance.
    public List<TypeNode> getTypes() {
        return List.copyOf(types);
    }

    /// Retrieves a package matching the specified qualified name.
    /// @param qualifiedName the fully qualified name of the package.
    /// @return the matching PackageNode if found, or null otherwise.
    public PackageNode getPackageNode(String qualifiedName) {
        for (PackageNode packageNode : packages) {
            if (packageNode.getName().equals(qualifiedName)){
                return packageNode;
            }
        }
        return null;
    }

    /// Gets the list of records *owned* by this instance.
    public List<TypeNode> getRecords() {
        List<TypeNode> out = new ArrayList<>();
        for (TypeNode t : types)
            if (t.isRecord())
                out.add(t);
        return out;
    }

    /// Retrieves a TypeNode based on its fully qualified name.
    /// @param qualifiedName the fully qualified name of the type.
    /// @return the matching TypeNode if found, or null otherwise.
    public TypeNode getTypeNode(String qualifiedName) {
        for (TypeNode typeNode : types) {
            if (typeNode.getQualifiedName().equals(qualifiedName)){
                return typeNode;
            }
        }
        return null;
    }

    /// Sorts the types in descending order by qualified name and sorts all child types recursively.
    public void sort() {
        packages.sort((o1, o2) -> o2.getName().compareTo(o1.getName()));
        for (PackageNode pkg : getPackages()) {
            pkg.sort();
        }
        for (TypeNode type : getTypes()) {
            type.sort();
        }
    }

    /// Computes the longest common base package prefix shared by all packages in this API.
    ///
    /// This method iterates through the list of packages and determines the common package name prefix,
    /// truncated at the nearest dot ('.') boundary. If no common base exists, returns an empty string.
    ///
    /// @return The longest common base package name shared by all packages, or an empty string if none.
    public String commonBase() {
        if (packages.isEmpty()) return "";
        int lastDot = 0;
        String base = packages.getFirst().getName();
        for (PackageNode pkg : packages) {
            String pkgName = pkg.getName();
            for (int j=0; j<Math.min(base.length(), pkgName.length()); j++) {
                if (base.charAt(j) != pkgName.charAt(j)) {
                    base = base.substring(0, lastDot);
                    break;
                }
                if (j == pkgName.length() - 1) {
                    base = base.substring(0, lastDot);
                }
                if (j < base.length() && base.charAt(j) == '.') lastDot = j;
            }
        }
        return base;
    }
}
