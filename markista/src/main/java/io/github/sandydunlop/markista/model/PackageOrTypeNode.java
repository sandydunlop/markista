package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

public abstract class PackageOrTypeNode extends AbstractPackageMember {
    // store children by the interface type (no concrete TypeNode mention)
    protected final List<TypeView> types = new ArrayList<>();

    protected PackageOrTypeNode() {
    }

    public void addType(TypeView typeNode) {
        types.add(typeNode);
    }

    /// Gets the list of types *owned* by this instance.
    public List<TypeView> getTypes() {
        return List.copyOf(types);
    }

    /// Gets the list of classes *owned* by this instance.
    public List<TypeView> getClasses() {
        List<TypeView> out = new ArrayList<>();
        for (TypeView t : types)
            if (t.isClass())
                out.add(t);
        return out;
    }

    /// Gets the list of interfaces *owned* by this instance.
    public List<TypeView> getInterfaces() {
        List<TypeView> out = new ArrayList<>();
        for (TypeView t : types)
            if (t.isInterface())
                out.add(t);
        return out;
    }

    /// Gets the list of enums *owned* by this instance.
    public List<TypeView> getEnums() {
        List<TypeView> out = new ArrayList<>();
        for (TypeView t : types)
            if (t.isEnum())
                out.add(t);
        return out;
    }

    /// Gets the list of annotations *owned* by this instance.
    public List<TypeView> getAnnotations() {
        List<TypeView> out = new ArrayList<>();
        for (TypeView t : types)
            if (t.isAnnotation())
                out.add(t);
        return out;
    }

    /// Sorts the nodes owned by this instance into alphabetical order.
    public void sort() {
        types.sort((a, b) -> a.getSimpleName().compareTo(b.getSimpleName()));
    }
}
