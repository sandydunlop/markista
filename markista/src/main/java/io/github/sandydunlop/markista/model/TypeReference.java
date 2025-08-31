package io.github.sandydunlop.markista.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class TypeReference implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String fullTypeName = "";
    protected Link link;
    protected String rawTypeName = "";
    protected int arrayDimensions = 0;

    private TypeReference() {
        // Nothing to see here
    }

    public boolean isArray() {
        return arrayDimensions > 0;
    }

    public int arrayDimensions() {
        return arrayDimensions;
    }

    public static TypeReference to(String fullTypeName) {
        if (fullTypeName == null || fullTypeName.isEmpty()) {
            return new TypeReference();
        }
        TypeReference type = parse(fullTypeName);
        if (type instanceof Generic generic) {
            generic.setFullTypeName(fullTypeName);
            return generic;
        }
        TypeReference typeRef = new TypeReference();
        typeRef.fullTypeName = fullTypeName;
        typeRef.rawTypeName = type.rawTypeName;
        return type;
    }

    public String getRawTypeName() {
        return rawTypeName;
    }

    public Link getLink() {
        return link;
    }

    public void setFullTypeName(String name) {
        fullTypeName = name;
    }

    public String getFullTypeName() {
        return fullTypeName;
    }

    public Generic asGeneric() {
        if (this instanceof Generic parameterized) {
            return parameterized;
        }
        return null;
    }

    public Sequence asSequence() {
        if (this instanceof Sequence sequence) {
            return sequence;
        }
        return null;
    }

    public String toString() {
        return stringify(this);
    }

    private String stringify(TypeReference typeReference) {
        StringBuilder sb = new StringBuilder();
        switch (typeReference) {
            case Generic generic -> {
                sb.append(generic.rawTypeName);
                sb.append("<");
                sb.append(stringify(generic.params));
                sb.append(">");
            }
            case Sequence sequence -> {
                for (TypeReference element : sequence) {
                    if (!sb.isEmpty()) {
                        sb.append(",");
                    }
                    sb.append(stringify(element));
                }
            }
            case TypeParameter typeParameter -> {
                if (typeParameter.extendsWildcard) {
                    sb.append("? extends ");
                }
                sb.append(typeParameter.rawTypeName);
            }
            default -> sb.append(typeReference.rawTypeName);
        }
        for (int d = 0; d < typeReference.arrayDimensions; d++) {
            sb.append("[]");
        }
        return sb.toString();
    }

    private static TypeParameter parse(String typeName) {
        if (typeName.indexOf('<') > -1) {
            return parseGeneric(typeName);
        } else if (typeName.indexOf(',') > -1) {
            return parseSequence(typeName, null);
        }
        return new TypeParameter(typeName);
    }

    private static TypeParameter parseSequence(String before, TypeParameter inner) {
        int comma = before.lastIndexOf(",");
        String beforeComma = before.substring(0, comma);
        String typeName = before.substring(comma + 1);
        TypeParameter typeRefAfterComma;

        if (inner != null) {
            Generic generic = new Generic(typeName);
            generic.params = inner;
            typeRefAfterComma = generic;
        } else {
            typeRefAfterComma = parse(typeName);
        }

        TypeParameter typeRefBeforeComma = parse(beforeComma);
        Sequence sequence = new Sequence();
        sequence.append(typeRefBeforeComma);
        sequence.append(typeRefAfterComma);
        return sequence;
    }

    private static TypeParameter parseGeneric(String str) {
        int openingChevron = str.indexOf("<");
        int closingChevron = str.lastIndexOf(">");
        String before = str.substring(0, openingChevron).strip();
        String mid = str.substring(openingChevron + 1, closingChevron).strip();
        String after = str.substring(closingChevron + 1).strip();
        TypeParameter inner = null;

        if (mid.contains("<")) {
            inner = parseGeneric(mid);
        } else if (mid.contains(",")) {
            inner = parseSequence(mid, null);
        } else {
            inner = parse(mid);
        }

        if (before.contains(",")) {
            return parseSequence(before, inner);
        }

        Generic generic = new Generic(before);
        generic.params = inner;
        parseArray(after, generic);
        return generic;
    }

    private static void parseArray(String str, TypeParameter type) {
        int pos = 0;
        while (pos < str.length()) {
            char c = str.charAt(pos);
            if (c == ']' || Character.isWhitespace(c)) {
                pos++;
            } else if (c == '[') {
                type.arrayDimensions++;
                pos++;
            } else {
                break;
            }
        }
    }

    public static class TypeParameter extends TypeReference {
        private boolean extendsWildcard;
        private boolean wildcard;

        protected TypeParameter() {
            // Nothing to see here
        }

        protected TypeParameter(String typeName) {
            if (typeName.contains("?")) {
                int e = typeName.indexOf("extends");
                if (e > -1) {
                    typeName = typeName.substring(e + 8).strip();
                    extendsWildcard = true;
                } else {
                    wildcard = true;
                }
            }
            int bracket = typeName.indexOf("[");
            if (bracket > -1) {
                rawTypeName = typeName.substring(0, bracket);
                parseArray(typeName.substring(bracket), this);
            } else {
                rawTypeName = typeName;
            }
            link = Link.to(typeName).withLabel(typeName);
        }

        public boolean hasExtendsWildcard() {
            return extendsWildcard;
        }

        public boolean hasWildcard() {
            return wildcard;
        }
    }

    public static class Generic extends TypeParameter {
        private TypeParameter params;

        protected Generic(String rawTypeName) {
            super(rawTypeName);
        }

        public TypeParameter getParams() {
            return params;
        }
    }

    public static class Sequence extends TypeParameter implements Iterable<TypeParameter> {
        private TypeParameter[] elements;
        private int size;
        private static final int INITIAL_CAPACITY = 4;

        protected Sequence() {
            elements = new TypeParameter[INITIAL_CAPACITY];
            size = 0;
        }

        private void resize() {
            int newCapacity = elements.length * 2;
            TypeParameter[] newArray = new TypeParameter[newCapacity];
            System.arraycopy(elements, 0, newArray, 0, elements.length);
            elements = newArray;
        }

        public int size() {
            return size;
        }

        public void append(TypeParameter element) {
            if (element instanceof Sequence sequence) {
                for (TypeParameter typeRef : sequence) {
                    append(typeRef);
                }
            } else {
                if (size == elements.length) {
                    resize();
                }
                elements[size++] = element;
            }
        }

        public TypeParameter get(int index) {
            if (index >= size) {
                throw new NoSuchElementException();
            }
            return elements[index];
        }

        public TypeParameter getFirst() {
            return get(0);
        }

        public TypeParameter getLast() {
            return get(size - 1);
        }

        @Override
        public Iterator<TypeParameter> iterator() {
            return new SequenceIterator();
        }

        private class SequenceIterator implements Iterator<TypeParameter> {
            private int currentIndex = 0;

            @Override
            public boolean hasNext() {
                return currentIndex < size;
            }

            @Override
            public TypeParameter next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return elements[currentIndex++];
            }
        }
    }
}
