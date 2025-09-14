package io.github.sandydunlop.markista.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;

/// See Java Language Specification (JLS) [Chapter 6. Names](https://docs.oracle.com/javase/specs/jls/se24/html/jls-6.html).
public class Name implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String[] components = new String[0];
    int packageComponentCount = 0;
    boolean isPackage = false;
    boolean isType = false;
    boolean isMember = false;

    /// Represents a name.
    public Name() {
        // Nothing to see here
    }

    public Name(String n) {
        this(n, null);
    }

    /// @param fqn fully qualified name
    /// @param pn package name
    public Name(String fqn, String pn) {
        this(null, fqn, pn);
    }

    /// @param mn simple method name
    /// @param fqn fully qualified name
    /// @param pn package name
    public Name(String mn, String fqn, String pn) {
        if (fqn == null || fqn.isEmpty()) {
            if (pn == null || pn.isEmpty()) {
                if (mn == null || mn.isEmpty()) {
                    return;
                }
            } else {
                fqn = pn;
                isPackage = true;
            }
        }
        if (mn != null) {
            if (fqn != null && !fqn.isEmpty()) {
                fqn += "." + mn;
            } else {
                fqn = mn;
            }
            isPackage = false;
            isMember = true;
        }
        validateName(fqn);
        int dollar = fqn.indexOf('$');
        if (dollar > -1) {
            String start = fqn.substring(0, dollar);
            int dot = start.lastIndexOf('.');
            pn = fqn.substring(0, dot);
            fqn = fqn.replace("$", ".");
        }
        if (pn == null || pn.isEmpty()) {
            components = extractComponents(fqn);
        } else {
            components = extractComponents(fqn);
            packageComponentCount = extractComponents(pn).length;
            if (mn == null && !isPackage) {
                isType = true;
            }
        }
    }

    private static void validateName(String name) {
        for (int i=0; i<name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isLetterOrDigit(c) && c!='_' && c!='.' && c!='$') {
                throw new IllegalArgumentException("\"" + name + "\" is not a valid name");
            }
        }
    }

    public Name(Name n) {
        this.components = Arrays.copyOf(n.components, n.components.length);
    }

    public void setIsPackage(boolean b) {
        isPackage = b;
        isMember = false;
        isType = false;
    }

    public boolean isPackage() {
        return isPackage;
    }

    public void setIsType(boolean b) {
        isType = b;
        isMember = false;
        isPackage = false;
    }

    public boolean isType() {
        return isType;
    }

    public void setIsMember(boolean b) {
        isMember = b;
        isPackage = false;
        isType = false;
    }

    public boolean isMember() {
        return isMember;
    }

    public boolean isEmpty() {
        return components.length == 0;
    }

    public void setPackageComponentCount(int n) {
        if (n < 0 || n > components.length) {
            throw new IllegalArgumentException("Package component count is out of bounds.");
        }
        packageComponentCount = n;
    }

    public int packageComponentCount() {
        return packageComponentCount;
    }

    public Name packageName() {
        if (components.length == 0) return new Name();
        Name name = firstComponents(packageComponentCount);
        name.isPackage = true;
        return name;
    }

    public Name typeName() {
        // A.B.C
        if (isPackage) return new Name();
        Name qualifiedTypeName = this;
        if (isMember) {
            qualifiedTypeName = firstComponents(-1);
        }
        if (qualifiedTypeName.packageComponentCount > 0) {
            return qualifiedTypeName.lastComponents(-packageComponentCount);
        }
        return this;
    }

    public Name memberName() {
        if (!isMember) return new Name();
        Name name = lastComponents(1);
        name.isMember = true;
        return name;
    }

    public String fullyQualifiedName() {
        // p.A.B.C
        if (components.length == 0) {
            return "";
        }
        return String.join(".", components);
    }

    public String simpleName() {
        // C
        return lastComponents(1).toString();
    }

    public String fullyQualifiedBinaryName() {
        // p.A$B$C
        if (packageComponentCount > 0) {
            return packageName().toString() + "." + simpleBinaryName();
        } else {
            // Can't tell what is package, type, or nested type
            return fullyQualifiedName();
        }
    }

    public String simpleBinaryName() {
        // A$B$C
        return typeName().toString().replace(".", "$");
    }

    @Override
    public String toString() {
        return fullyQualifiedName();
    }

    public int componentCount() {
        return components.length;
    }

    public int commonComponentCount(Name n) {
        if (n == null) {
            return 0;
        }
        int commonCount = 0;
        while (commonCount < Math.min(components.length, n.componentCount()) &&
                components[commonCount].equals(n.components[commonCount])) {
            commonCount++;
        }
        return commonCount;
    }

    public Name firstComponents(int n) {
        if (n > components.length) {
            throw new IndexOutOfBoundsException(String.format(
                    "%d is out of bounds for 0 to %d", n, components.length));
        }
        if (n == 0) {
            return new Name(this);
        }
        StringBuilder sb = new StringBuilder();
        int end = n < 0 ? components.length + n : n;
        for (int i = 0; i < end; i++) {
            if (!sb.isEmpty()) {
                sb.append(".");
            }
            sb.append(components[i]);
        }
        Name name = new Name(sb.toString());
        if (end <= packageComponentCount) {
            name.packageComponentCount = n;
            name.isPackage = true;
        } else {
            name.packageComponentCount = packageComponentCount;
            if (isMember && end == components.length) {
                name.isMember = true;
            } else {
                name.isType = true;
            }
        }
        return name;
    }

    public Name lastComponents(int n) {
        if (n > components.length) {
            throw new IndexOutOfBoundsException(String.format(
                    "%d is out of bounds for 0 to %d", n, components.length));
        }
        if (n == 0) {
            return new Name(this);
        }
        StringBuilder sb = new StringBuilder();
        int start = n < 0 ? -n : components.length - n;
        for (int i = start; i < components.length; i++) {
            if (!sb.isEmpty()) {
                sb.append(".");
            }
            sb.append(components[i]);
        }
        Name name = new Name(sb.toString());
        name.packageComponentCount = packageComponentCount - start;
        name.isPackage = isPackage;
        name.isType = isType;
        name.isMember = isMember;
        return name;
    }




    private String[] extractComponents(String s) {
        String[] c = s.split("\\.");
        if (c.length == 1 && c[0].isBlank()) {
            return new String[0];
        } else {
            return c;
        }
    }
}
