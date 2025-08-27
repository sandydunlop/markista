package io.github.sandydunlop.markista.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TypeRef implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Link link;
    private Text text;
    private String qualifiedName;

    //
    //
    //
    protected String typeString;

    public String getTypeString() {
        return typeString;
    }

    private TypeRef() {
        // Nothing to see here
    }

    public static TypeRef to(String typeName) {
        TypeRef typeRef = parse(typeName);
        return typeRef;
        // TypeRef ref = new TypeRef();
        // ref.setLink(Link.to(typeName));
        // ref.setText(Text.empty());
        // ref.setQualifiedName(typeName);
        // return ref;
    }

    public static TypeRef empty() {
        return new TypeRef();
    }

    public void setLink(Link ref) {
        link = ref;
    }

    public Link getLink() {
        return link;
    }

    public void setText(Text text) {
        this.text = text;
    }

    public Text getText() {
        return text;
    }

    public void setQualifiedName(String name) {
        qualifiedName = name;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    //
    //
    //

    public TypeRef.Array asArray() {
        if (this instanceof TypeRef.Array array) {
            return array;
        }
        return null;
    }

    public TypeRef.Generic asGeneric() {
        if (this instanceof TypeRef.Generic generic) {
            return generic;
        }
        return null;
    }

    public TypeRef.Sequence asSequence() {
        if (this instanceof TypeRef.Sequence sequence) {
            return sequence;
        }
        return null;
    }

    public static TypeRef parse(String targetName) {
        String pre = "";
        String post = "";
        int pos;

        if (targetName.indexOf('<') > -1) {

            // TypeReference typeRef = linkGenerics2(targetName);
            TypeRef typeRef = linkGenerics(targetName);
            return typeRef;
        } else if (targetName.indexOf(',') > -1) {
            return splitAndLink(targetName);
        }
        //  else if (targetName.lastIndexOf(' ') > 0) {
        //     int p = targetName.lastIndexOf(' ');
        //     pre = targetName.substring(0, p) + " ";
        //     targetName = targetName.substring(p + 1);
        // }

        pos = targetName.indexOf('[');
        if (pos > 0) {
            TypeRef.Array array = TypeRef.Array.empty();
            array.typeString = targetName.substring(0, pos);
            array.dimensions = 0;

            while (pos < targetName.length()) {
                if (targetName.charAt(pos) == '[') {
                    array.dimensions++;
                }
                pos++;
            }

            return array;
        }


        TypeRef typeRef = new TypeRef();
        typeRef.typeString = targetName;
        return typeRef;
    }

    public static TypeRef linkGenerics(String str) {
        if (str == null || str.isEmpty()) return TypeRef.empty();
        int openingChevron = str.indexOf("<");
        int closingChevron = str.lastIndexOf(">");
        String before = str.substring(0, openingChevron);
        String mid = str.substring(openingChevron + 1, closingChevron);
        String after = str.substring(closingChevron + 1);
        //TODO: after could be array brackets?

        TypeRef.Generic generic = new TypeRef.Generic();
        generic.typeString = before;

        // TypeRef midLinks;
        if (mid.contains("<")) {
            generic.params = linkGenerics(mid);
        } else {
            generic.params = splitAndLink(mid);
        }

        // genericRef.params = midLinks;
        return generic;
    }

    public static TypeRef splitAndLink(String typesString) {
        String[] types = typesString.split(",");
        if (types.length > 1) {
            TypeRef.Sequence sequence = TypeRef.Sequence.empty();
            for (String t : types) {
                String typeName = t.strip();
                TypeRef typeRef = TypeRef.to(typeName);
                sequence.add(typeRef);
            }
            return sequence;
        } else {
            return TypeRef.to(typesString.strip());
        }
    }

    //
    //
    //
    public static class Generic extends TypeRef {
        // private TypeRef type;
        // private TypeRef.Sequence params;
        private TypeRef params;
        // private List<TypeRef> params = new ArrayList<>();

        public Generic() {
        }

        public static Generic empty() {
            return new Generic();
        }

        // public TypeRef getType() {
        //     return type;
        // }

        public TypeRef getParams() {
            return params;
        }
    }

    public static class Array extends TypeRef {
        private int dimensions;

        private Array() {
        }

        public static Array empty() {
            return new Array();
        }

        public int getDimensions() {
            return dimensions;
        }
    }

    public static class Sequence extends TypeRef {
        private List<TypeRef> items = new ArrayList<>();
        
        private Sequence() {
        }

        public static Sequence empty() {
            return new Sequence();
        }

        public void add(TypeRef typeRef) {
            items.add(typeRef);
        }
    }
}
