package io.github.sandydunlop.markista.model;

import java.io.Serial;
import java.io.Serializable;

public class Reference implements Serializable{
    @Serial
    private static final long serialVersionUID = 1L;

    Name name;
    String module = "";
    String parameters = "";

    public Reference(String n) {
        if (n == null || n.isBlank()) {
            throw new IllegalArgumentException("\"" + n + "\" is not a valid reference");
        }
        String packageOrType = "";
        String member = "";

        int slash = n.indexOf("/");
        if (slash > -1) {
            module = n.substring(0, slash);
            n = n.substring(slash + 1);
        }
        int hash = n.indexOf("#");
        if (hash > -1) {
            packageOrType = n.substring(0, hash);
            member = n.substring(hash + 1);
            int parenthesis = member.indexOf("(");
            if (parenthesis > -1) {
                parameters = member.substring(parenthesis);
                member = member.substring(0, parenthesis);
            }
            name = new Name(member, packageOrType, null);
            name.setIsMember(true);
        } else {
            if (!n.isBlank()) {
                packageOrType = n;
                name = new Name(packageOrType);
            }
        }
    }

    public Reference(String m, Name n) {
        this.module = m;
        this.name = n;
    }

    public void setModule(String n) {
        module = n;
    }

    public String getModuleName() {
        return module;
    }

    public void setName(Name n) {
        name = n;
    }

    public Name getName() {
        return name;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (module != null && !module.isEmpty()) {
            sb.append(module);
            sb.append("/");
        }
        if (name != null && !name.isEmpty()) {
            sb.append(name);
        }
        sb.append(parameters);
        return sb.toString();
    }
}
