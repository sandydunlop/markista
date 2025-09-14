package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

/// A node representing a Java interface type
public class InterfaceNode extends TypeNode {

    private List<Link> implementingClasses = new ArrayList<>();

    /// Create an InterfaceNode with the specified details
    /// @param name The name of the enum.
    public InterfaceNode(Name name) {
        super(name);
        kind = Node.Kind.INTERFACE;
    }

    public void addImplementingClass(Link classLink) {
        implementingClasses.add(classLink);
    }

    public List<Link> getImplementingClasses() {
        return implementingClasses;
    }
}
