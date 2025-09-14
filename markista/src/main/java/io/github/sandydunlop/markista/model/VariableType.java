package io.github.sandydunlop.markista.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;

///
/// See Java Language Specification (JLS) [Chapter 4. Types, Values, and Variables](https://docs.oracle.com/javase/specs/jls/se24/html/jls-4.html).
public class VariableType implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String fullTypeName = "";
    protected Link link = null;
    protected String rawTypeName = "";
    protected int arrayDimensions = 0;
    private VariableType typeParameterDeclaration = null;

    private VariableType() {
        // Nothing to see here
    }

    public boolean isArray() {
        return arrayDimensions > 0;
    }

    public int arrayDimensions() {
        return arrayDimensions;
    }

    public static VariableType parse(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return new VariableType();
        }
        VariableType typeParam = null;
        String fullTypeName = typeName.strip();
        if (fullTypeName.startsWith("<")) {
            int closingTypeParamChevron = matchingClosingChevron(fullTypeName, 0);
            String typeParamString = fullTypeName.substring(0, closingTypeParamChevron + 1);
            typeParam = parseGeneric(typeParamString.strip());
            fullTypeName = fullTypeName.substring(closingTypeParamChevron + 1).strip();
        }

        VariableType type = parseType(fullTypeName);
        type.typeParameterDeclaration = typeParam;
        if (type instanceof Generic generic) {
            generic.setFullTypeName(fullTypeName);
            return generic;
        }

        // Hide whatever subtype `type` might be...
        VariableType variableType = new VariableType();
        variableType.fullTypeName = fullTypeName;
        variableType.rawTypeName = type.rawTypeName;
        variableType.typeParameterDeclaration = typeParam;
        variableType.link = type.link;
        variableType.fullTypeName = fullTypeName;
        variableType.arrayDimensions = type.arrayDimensions;
        return variableType;
    }

    private static int matchingClosingChevron(String string, int pos) {
        int depth = 0;
        for (; pos < string.length(); pos++) {
            char c = string.charAt(pos);
            if (c=='<') {
                depth++;
            } else if (c== '>') {
                depth--;
            }
            if (depth == 0) break;
        }
        return pos;
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

    public VariableType getTypeParameterDeclaration() {
        return typeParameterDeclaration;
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

    private String stringify(VariableType typeReference) {
        StringBuilder sb = new StringBuilder();
        if (typeReference.typeParameterDeclaration != null) {
            sb.append(typeReference.typeParameterDeclaration);
            sb.append(" ");
        }
        switch (typeReference) {
            case Generic generic -> {
                sb.append(generic.rawTypeName);
                sb.append("<");
                sb.append(boundingConstraint(generic));
                sb.append(stringify(generic.params));
                sb.append(">");
            }
            case Sequence sequence -> {
                for (VariableType element : sequence) {
                    if (!sb.isEmpty()) {
                        sb.append(",");
                    }
                    sb.append(stringify(element));
                }
            }
            case TypeParameter typeParameter -> {
                sb.append(boundingConstraint(typeParameter));
                sb.append(typeParameter.rawTypeName);
            }
            default -> sb.append(typeReference.rawTypeName);
        }
        for (int d = 0; d < typeReference.arrayDimensions; d++) {
            sb.append("[]");
        }
        return sb.toString();
    }

    private static String boundingConstraint(TypeParameter type) {
        if (type.getBoundingKind() == BoundingKind.UPPER) {
            return type.getBoundingParameter() + " extends ";
        } else if (type.getBoundingKind() == BoundingKind.LOWER) {
            return type.getBoundingParameter() + " super ";
        }
        return "";
    }

    private static TypeParameter parseType(String str) {
        int lastComma = str.lastIndexOf(",");
        int lastChevron = str.lastIndexOf(">");
        if (lastComma > -1 && lastChevron > -1) {
            if (lastComma > lastChevron) {
                return parseSequence(str, null);
            } else {
                return parseGeneric(str);
            }
        } else if (lastComma > -1) {
            return parseSequence(str, null);
        } else if (lastChevron > -1) {
            return parseGeneric(str);
        }
        return new TypeParameter(str);
    }

    private static TypeParameter parseSequence(String before, TypeParameter inner) {
        int comma = before.lastIndexOf(",");
        String beforeComma = before.substring(0, comma).strip();
        String typeName = before.substring(comma + 1).strip();
        TypeParameter typeRefAfterComma;

        if (inner != null) {
            Generic generic = new Generic(typeName);
            generic.params = inner;
            typeRefAfterComma = generic;
        } else {
            typeRefAfterComma = parseType(typeName);
        }

        TypeParameter typeRefBeforeComma = parseType(beforeComma);
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

        // `T extends` is called a type bound. It is a constraint rather than a statement or expression.
        // `<T extends Type>` is a type parameter declaration
        Pair<String,String> upperBound = parseBoundingConstraint("extends", mid);
        Pair<String,String> lowerBound = parseBoundingConstraint("super", mid);
        BoundingKind boundingType = BoundingKind.NONE;
        String typeParameter = "";
        if (upperBound != null) {
            boundingType = BoundingKind.UPPER;
            typeParameter = upperBound.getL();
            mid = upperBound.getR();
        } else if (lowerBound != null) {
            boundingType = BoundingKind.LOWER;
            typeParameter = lowerBound.getL();
            mid = lowerBound.getR();
        }

        inner = parseType(mid);

        if (before.contains(",")) {
            return parseSequence(before, inner);
        }

        Generic generic = new Generic(before);
        generic.params = inner;
        generic.boundingKind = boundingType;
        generic.boundingParameter = typeParameter;
        parseArray(after, generic);
        return generic;
    }

    private static Pair<String,String> parseBoundingConstraint(String keyword, String type) {
        String nameOrWildcard = "";
        String boundingType = "";
        int nameStart = -1;
        int boundingTypeStart = -1;
        int pos = 0;
        while (pos < type.length()) {
            char c = type.charAt(pos);
            if (nameStart == -1 && !Character.isWhitespace(c)) {
                nameStart = pos;
            } else if (nameStart > -1 && boundingTypeStart == -1 && Character.isWhitespace(c)) {
                nameOrWildcard = type.substring(nameStart, pos);
            } else if (boundingTypeStart == -1 && !Character.isWhitespace(c)) {
                boundingTypeStart = pos;
            } else if (boundingTypeStart > -1 && Character.isWhitespace(c)) {
                boundingType = type.substring(boundingTypeStart, pos);
                if (boundingType.equals(keyword)) {
                    String remainder = type.substring(pos + 1);
                    return Pair.of(nameOrWildcard,remainder);
                }
            }
            pos++;
        }
        return null;
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

    public enum BoundingKind {
        NONE,
        UPPER,
        LOWER
    }

    public static class TypeParameter extends VariableType {
        protected String boundingParameter = null;
        protected BoundingKind boundingKind = BoundingKind.NONE;

        protected TypeParameter() {
            // Nothing to see here
        }

        protected TypeParameter(String str) {
            if (str.isEmpty()) return;

            Pair<String,String> upperBound = parseBoundingConstraint("extends", str);
            Pair<String,String> lowerBound = parseBoundingConstraint("super", str);
            BoundingKind boundingType = BoundingKind.NONE;
            String typeParameter = "";
            if (upperBound != null) {
                boundingType = BoundingKind.UPPER;
                typeParameter = upperBound.getL();
                str = upperBound.getR();
            } else if (lowerBound != null) {
                boundingType = BoundingKind.LOWER;
                typeParameter = lowerBound.getL();
                str = lowerBound.getR();
            }
            this.boundingKind = boundingType;
            this.boundingParameter = typeParameter;

            int bracket = str.indexOf("[");
            if (bracket > -1) {
                rawTypeName = str.substring(0, bracket);
                parseArray(str.substring(bracket), this);
            } else {
                rawTypeName = str;
            }
            if (!rawTypeName.equals("?")) {
                try {
                    Reference ref = new Reference(rawTypeName);
                    link = Link.to(ref);
                } catch (IllegalArgumentException _) {
                    // This should never happen
                }
            }
        }

        public String getBoundingParameter() {
            return boundingParameter;
        }

        public BoundingKind getBoundingKind() {
            return boundingKind;
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
