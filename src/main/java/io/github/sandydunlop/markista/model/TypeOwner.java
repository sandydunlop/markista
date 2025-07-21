package io.github.sandydunlop.markista.model;

import java.util.List;

public interface TypeOwner {
    public void addType(TypeNode typeNode);
    public List<TypeNode> getTypes();
    public List<PackageMember> getClasses();
    public List<PackageMember> getInterfaces();
    public List<PackageMember> getEnums();
    public List<PackageMember> getAnnotations();
    public String getName();
}
