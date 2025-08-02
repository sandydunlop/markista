package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import javax.lang.model.element.TypeElement;

/// Represents the API being documented, encapsulating its modules and packages.
/// Provides methods to add and retrieve modules, packages, and types, as well as sorting them.
public class Api extends AbstractTypeOwner {
    /// List of modules included in the API.
    private List<ModuleNode> modules = new ArrayList<>();

    /// List of packages included in the API.
    private List<PackageNode> packages = new ArrayList<>();
    
    /// Represents the unnamed module in the API.
    private ModuleNode unnamedModule = new ModuleNode("");

    /// Constructs an empty Api instance.
    public Api() {
        // Nothing to see here
    }

    /// Returns the name of the API.
    /// @return The name of the API
    public String getName() {
        return simpleName;
    }

    /// Sets the name of the API.
    /// @param name The name of the API
    public void setName(String name) {
        simpleName = name;
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
            if (moduleNode.qualifiedName.equals(qualifiedName)){
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

    /// Retrieves a package matching the specified qualified name.
    /// @param qualifiedName the fully qualified name of the package.
    /// @return the matching PackageNode if found, or null otherwise.
    public PackageNode getPackageNode(String qualifiedName) {
        for (PackageNode packageDoc : packages) {
            if (packageDoc.qualifiedName.equals(qualifiedName)){
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

    /// Retrieves a TypeNode based on a TypeElement.
    /// @param type the TypeElement to match.
    /// @return the matching TypeNode if found, or null if type is null or no match found.
    public TypeNode getTypeNode(TypeElement type) {
        if (type == null) return null;
        return getTypeNode(type.getQualifiedName().toString());
    }

    /// Sorts the packages in descending order by qualified name and sorts all child types recursively.
    @Override
    public void sort() {
        Collections.sort(packages, (o1, o2) -> o2.qualifiedName.compareTo(o1.qualifiedName) );
        for (TypeNode node : getTypes()) {
            node.sort();
        }
    }
}
