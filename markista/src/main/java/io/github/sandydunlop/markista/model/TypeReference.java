package io.github.sandydunlop.markista.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TypeReference implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Link link; // = new Link();
    private Text text;
    private String qualifiedName;

    //
    //
    //
    protected String typeString;

    public String getTypeString() {
        return typeString;
    }

    private TypeReference() {
        // Nothing to see here
    }

    public static TypeReference to(String typeName) {
        if (typeName == null) {
            typeName = null;
        }
        if (typeName.equals("java.util.HashMap<io.github.sandydunlop.markista.model.TypeReference,java.util.List<io.github.sandydunlop.markista.model.Link>>")){
            typeName=typeName;
        }
        TypeReference typeRef = parse(typeName);
        // typeRef.setLink(Link.to(typeName));
        typeRef.setQualifiedName(typeName);
        return typeRef;
        // TypeReference ref = new TypeReference();
        // ref.setLink(Link.to(typeName));
        // ref.setText(Text.empty());
        // ref.setQualifiedName(typeName);
        // return ref;
    }

    public static TypeReference empty() {
        return new TypeReference();
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

    public TypeReference.Array asArray() {
        if (this instanceof TypeReference.Array array) {
            return array;
        }
        return null;
    }

    public TypeReference.Generic asGeneric() {
        if (this instanceof TypeReference.Generic generic) {
            return generic;
        }
        return null;
    }

    public TypeReference.Sequence asSequence() {
        if (this instanceof TypeReference.Sequence sequence) {
            return sequence;
        }
        return null;
    }

    public static TypeReference parse(String targetName) {
        if (targetName == null || targetName.isEmpty()) {
            targetName=targetName;
        }
        String pre = "";
        String post = "";
        int pos;

        if (targetName.indexOf('<') > -1) {

            // TypeReferenceerence typeRef = linkGenerics2(targetName);
            TypeReference typeRef = linkGenerics(targetName);
            return typeRef;
        } else if (targetName.indexOf(',') > -1) {
            return splitAndLink(targetName);
        }
        //  else if (targetName.lastIndexOf(' ') > 0) {
        //     int p = targetName.lastIndexOf(' ');
        //     pre = targetName.substring(0, p) + " ";
        //     targetName = targetName.substring(p + 1);
        // }

        //TODO: Array need to happen even in generic params
        pos = targetName.indexOf('[');
        if (pos > 0) {
            TypeReference.Array array = TypeReference.Array.empty();
            array.typeString = targetName.substring(0, pos);
            array.dimensions = 0;

            while (pos < targetName.length()) {
                if (targetName.charAt(pos) == '[') {
                    array.dimensions++;
                }
                pos++;
            }
            array.setLink(Link.to(array.typeString));
            return array;
        }


        TypeReference typeRef = new TypeReference();
        typeRef.typeString = targetName;
        typeRef.setLink(Link.to(targetName)
                .withLabel(targetName));
        return typeRef;
    }

    public static TypeReference linkGenerics(String str) {
        if (str == null || str.isEmpty()) return TypeReference.empty();
        int openingChevron = str.indexOf("<");
        int closingChevron = str.lastIndexOf(">");
        String before = str.substring(0, openingChevron).strip();
        String mid = str.substring(openingChevron + 1, closingChevron).strip();
        String after = str.substring(closingChevron + 1).strip();
        //TODO: after could be array brackets?

        TypeReference.Generic generic = new TypeReference.Generic();

        // "? extends "
        if (mid.contains("?")) {
            int e = mid.indexOf("extends");
            if (e > -1) {
                mid = mid.substring(e + 8).strip();
                generic.extendsWildcard = true;
            } else {
                generic.wildcard = true;
            }
        }

        String genericType = before;
        int comma = before.lastIndexOf(",");
        if (comma > -1) {
            TypeReference.Sequence sequence = new TypeReference.Sequence();
            genericType = str.substring(comma + 1, openingChevron);
            before = str.substring(0, comma);

            // need to parse sequence then add this generic on the end
            TypeReference part1 = parse(before);
            sequence.add(part1);

            generic.typeString = genericType;
            generic.setLink(Link.to(generic.typeString)
                    .withLabel(generic.typeString));
            sequence.add(generic);

            // if (mid.contains("<")) {
                generic.params = parse(mid);
            // } 

            return sequence;
        } else {
            // TypeReference beforeRef = parse(before);

            generic.typeString = before;
            generic.params = parse(mid);

            // TypeReference midLinks;
            // if (mid.contains("<")) {
            //     generic.params = linkGenerics(mid);
            // } 
            generic.setLink(Link.to(generic.typeString)
                    .withLabel(generic.typeString));
            return generic;
        }
        // else {
        //     generic.params = splitAndLink(mid);
        // }

        // genericRef.params = midLinks;
    }

    public static TypeReference splitAndLink(String typesString) {
        String[] types = typesString.split(",");
        if (types.length > 1) {
            TypeReference.Sequence sequence = TypeReference.Sequence.empty();
            for (String t : types) {
                String typeName = t.strip();
                TypeReference typeRef = TypeReference.to(typeName);
                sequence.add(typeRef);
            }
            return sequence;
        } else {
            return TypeReference.to(typesString.strip());
        }
    }

    //
    //
    //
    public static class Generic extends TypeReference {
        // private TypeReference type;
        // private TypeReference.Sequence params;
        private TypeReference params;
        private boolean extendsWildcard;
        private boolean wildcard;
        // private List<TypeReference> params = new ArrayList<>();

        public Generic() {
        }

        public static Generic empty() {
            return new Generic();
        }

        // public TypeReference getType() {
        //     return type;
        // }

        public TypeReference getParams() {
            return params;
        }

        public boolean hasExtendsWildcard() {
            return extendsWildcard;
        }

        public boolean hasWildcard() {
            return wildcard;
        }
    }

    public static class Array extends TypeReference {
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

    public static class Sequence extends TypeReference {
        private List<TypeReference> items = new ArrayList<>();
        
        private Sequence() {
        }

        public static Sequence empty() {
            return new Sequence();
        }

        public void add(TypeReference typeRef) {
            items.add(typeRef);
        }

        public List<TypeReference> getItems() {
            return items;
        }
    }
}
