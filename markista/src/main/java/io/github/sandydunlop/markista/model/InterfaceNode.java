package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

/// A node representing a Java interface type
public class InterfaceNode extends TypeNode {

    private List<Link> implementingClasses = new ArrayList<>();
    
    /// Create an InterfaceNode with the specified details
    /// @param simpleName The simple name of the enum.
    /// @param packageName the name of the package that the interface is a member of
    public InterfaceNode(String simpleName, String packageName) {
        super(simpleName, packageName);
        kind = Node.Kind.INTERFACE;
    }

    public void addImplementingClass(Link classLink) {
        implementingClasses.add(classLink);
    }
    
    public List<Link> getImplementingClasses() {
        return implementingClasses;
    }
}
