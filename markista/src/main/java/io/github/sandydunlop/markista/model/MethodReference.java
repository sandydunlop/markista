package io.github.sandydunlop.markista.model;

import java.io.Serializable;

public class MethodReference implements Serializable{
    String signature;
    Link link = new Link();

    public MethodReference() {
        // Nothing to see here
    }

    public void setSignature(String s) {
        signature = s;
    }

    public String getSignature() {
        return signature;
    }

    public void setLink(Link l) {
        link = l;
    }

    public Link getLink() {
        return link;
    }
}
