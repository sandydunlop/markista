package io.github.sandydunlop.markista.model;

public class PackageReference extends Node {
    private String name;
    private Link link;

    public PackageReference(String n) {
        name = n;
        link = Link.to(new Reference(n));
    }

    public void setName(String n) {
        name = n;
    }

    public String getName() {
        return name;
    }

    public void setLink(Link l) {
        link = l;
    }

    public Link getLink() {
        return link;
    }
}
