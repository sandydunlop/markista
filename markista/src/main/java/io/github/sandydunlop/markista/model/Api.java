package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

/// Represents the API being documented, encapsulating its modules and packages.
/// Provides methods to add and retrieve modules, packages, and types, as well as sorting them.
public class Api extends AbstractTypeOwner {
    /// List of modules included in the API.
    private final List<ModuleNode> modules = new ArrayList<>();

    /// List of packages included in the API.
    private final List<PackageNode> packages = new ArrayList<>();
    
    /// Represents the unnamed module in the API.
    private final ModuleNode unnamedModule = new ModuleNode("");

    /// List of annotations applied to this type.
    private final List<AppliedAnnotationNode> appliedAnnotations = new ArrayList<>();

    /// Constructs an empty Api instance with the given name.
    /// @param name The name of the API
    public Api(String name) {
        simpleName = name;
    }

    /// Returns the name of the API.
    /// @return The name of the API
    public String getName() {
        return simpleName;
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

    /// Retrieves a package matching the specified qualified name.
    /// @param qualifiedName the fully qualified name of the package.
    /// @return the matching PackageNode if found, or null otherwise.
    public PackageNode getPackageNode(String qualifiedName) {
        for (PackageNode packageDoc : packages) {
            if (packageDoc.getName().equals(qualifiedName)){
                return packageDoc;
            }
        }
        return null;
    }

    /// Retrieves a TypeNode based on its fully qualified name.
    /// @param qualifiedName the fully qualified name of the type.
    /// @return the matching TypeNode if found, or null otherwise.
    public TypeNode getTypeNode(String qualifiedName) {
        if (types == null) return null;
        for (TypeNode typeNode : types) {
            if (typeNode.qualifiedName.equals(qualifiedName)){
                return typeNode;
            }
        }
        return null;
    }

    /// Sorts the packages in descending order by qualified name and sorts all child types recursively.
    @Override
    public void sort() {
        packages.sort((o1, o2) -> o2.getName().compareTo(o1.getName()));
        for (TypeNode node : getTypes()) {
            node.sort();
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
