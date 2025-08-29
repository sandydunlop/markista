package io.github.sandydunlop.markista.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class TypeReference implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Link link;
    private Text text;
    private String qualifiedName;
    protected String typeString;

    private TypeReference() {
        // Nothing to see here
    }

    public static TypeReference to(String typeName) {
        TypeReference typeRef = parse(typeName);
        typeRef.setQualifiedName(typeName);
        return typeRef;
    }

    public static TypeReference empty() {
        return new TypeReference();
    }

    public String getTypeString() {
        return typeString;
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

    private static TypeReference parse(String targetName) {
        int pos;

        if (targetName.indexOf('<') > -1) {
            return linkGenerics(targetName);
        } else if (targetName.indexOf(',') > -1) {
            return splitAndLink(targetName);
        }


        pos = targetName.indexOf('[');
        if (pos > 0) {
            TypeReference.Array array = new TypeReference.Array();
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

    private static TypeReference linkGenerics(String str) {
        if (str == null || str.isEmpty()) return TypeReference.empty();
        int openingChevron = str.indexOf("<");
        int closingChevron = str.lastIndexOf(">");
        String before = str.substring(0, openingChevron).strip();
        String mid = str.substring(openingChevron + 1, closingChevron).strip();

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

        String genericType;
        int comma = before.lastIndexOf(",");
        if (comma > -1) {
            TypeReference.Sequence sequence = new TypeReference.Sequence();
            genericType = str.substring(comma + 1, openingChevron);
            before = str.substring(0, comma);

            TypeReference part1 = parse(before);
            sequence.add(part1);

            generic.typeString = genericType;
            generic.setLink(Link.to(generic.typeString)
                    .withLabel(generic.typeString));
            sequence.add(generic);

            generic.params = parse(mid);

            return sequence;
        } else {
            generic.typeString = before;
            generic.params = parse(mid);
            generic.setLink(Link.to(generic.typeString)
                    .withLabel(generic.typeString));
            return generic;
        }
    }

    public static TypeReference splitAndLink(String typesString) {
        String[] types = typesString.split(",");
        if (types.length > 1) {
            TypeReference.Sequence sequence = new TypeReference.Sequence();
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

    public static class Generic extends TypeReference {
        private TypeReference params;
        private boolean extendsWildcard;
        private boolean wildcard;

        protected Generic() {
            // Nothing to see here
        }

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

        protected Array() {
            // Nothing to see here
        }

        public int getDimensions() {
            return dimensions;
        }
    }

    public static class Sequence extends TypeReference implements Iterable<TypeReference> {
        private TypeReference[] elements;
        private int size;
        private static final int INITIAL_CAPACITY = 4;

        protected Sequence() {
            elements = new TypeReference[INITIAL_CAPACITY];
            size = 0;
        }

        private void resize() {
            int newCapacity = elements.length * 2; // Double the capacity
            TypeReference[] newArray = new TypeReference[newCapacity];
            System.arraycopy(elements, 0, newArray, 0, elements.length);
            elements = newArray;
        }

        public void add(TypeReference element) {
            if (size == elements.length) {
                resize(); // Resize the array if needed
            }
            elements[size++] = element;
        }

        public TypeReference get(int index) {
            if (index >= size) {
                throw new NoSuchElementException();
            }
            return elements[index];
        }

        public TypeReference getFirst() {
            return get(0);
        }

        @Override
        public Iterator<TypeReference> iterator() {
            return new SequenceIterator();
        }

        private class SequenceIterator implements Iterator<TypeReference> {
            private int currentIndex = 0;

            @Override
            public boolean hasNext() {
                return currentIndex < size;
            }

            @Override
            public TypeReference next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return elements[currentIndex++];
            }
        }
    }
}
