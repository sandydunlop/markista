package io.github.sandydunlop.markista.util;

import com.sun.source.doctree.DeprecatedTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTree.Kind;
import com.sun.source.doctree.ErroneousTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.doctree.SinceTree;
import com.sun.source.doctree.StartElementTree;

import io.github.sandydunlop.markista.model.AnnotationNode;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ClassNode;
import io.github.sandydunlop.markista.model.Deprecation;
import io.github.sandydunlop.markista.model.EnumNode;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.InterfaceNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.Node;
import io.github.sandydunlop.markista.model.OverriddenMethodNode;
import io.github.sandydunlop.markista.model.PackageMember;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.Reference;
import io.github.sandydunlop.markista.model.Text;
import io.github.sandydunlop.markista.model.TypeNode;
import io.github.sandydunlop.markista.model.Text.SegmentKind;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

import jdk.javadoc.doclet.DocletEnvironment;

import static javax.lang.model.element.Modifier.*;


public class TypeUtils {
    private static DocletEnvironment environment;
    private static Api api;

    private TypeUtils() {
        // Hide public constructor
    }

    public static void init(Api a, DocletEnvironment e) {
        api = a;
        environment = e;
    }

    public static TypeNode nodeFromElement(TypeElement element) {
        String qualifiedName = element.getQualifiedName().toString();
        TypeNode typeNode = api.getTypeNode(qualifiedName);
        if (typeNode == null) {
            PackageElement packageElement = getEnclosingPackageElement(element);
            String simpleName = element.getSimpleName().toString();
            if (packageElement == null) {
                Configuration.getReporter().print(Diagnostic.Kind.ERROR, "No package for " + qualifiedName);
                return null;
            }
            PackageNode packageNode = api.getPackageNode(packageElement.getQualifiedName().toString());
            typeNode = createTypeNode(qualifiedName, simpleName, packageNode, element.getKind());
            if (typeNode == null) {
                Configuration.getReporter().print(Diagnostic.Kind.ERROR, "Unsupported type kind: " + element.getKind());
                return null;
            }
            if (typeNode instanceof EnumNode enumNode) {
                setEnumConstants(enumNode, element);
            }
            setTypeOwnership(typeNode, element);
            setModifiers(typeNode, element.getModifiers());
            collectAllSupertypes(element.asType(), typeNode.getSupertypes());
            typeNode.getSupertypes().add(0, "java.lang.Object");
            findImplementedInterfaces(element, typeNode.getImplementedInterfaces());
            api.addType(typeNode);
        }
        return typeNode;
    }

    public static void setEnumConstants(EnumNode enumNode, TypeElement e) {
        List<? extends Element> enclosedElements = e.getEnclosedElements();
        for (Element element : enclosedElements) {
            if (element.getKind() == ElementKind.ENUM_CONSTANT) {
                FieldNode enumConstant = new FieldNode(null, element.getSimpleName().toString());
                enumNode.getConstants().add(enumConstant);
            }
        }
    }

    public static MethodNode nodeFromElement(ExecutableElement element) {
        TypeMirror typeMirror = element.getReturnType();
        String qualifiedTypeName = typeMirror.toString();
        String arrayBrackets = "";
        if (typeMirror.getKind() == TypeKind.ARRAY) {
            TypeMirror array = ((ArrayType)typeMirror).getComponentType();
            qualifiedTypeName = array.toString();
            arrayBrackets = "\\[]";                    
        }
        // The return type...
        String simpleName = Utils.simplifyNames(qualifiedTypeName);
        PackageElement packageElement = getEnclosingPackageElement(element);
        if (packageElement == null) {
            Configuration.getReporter().print(Diagnostic.Kind.ERROR, "No package for " + qualifiedTypeName);
            return null;
        }
        PackageNode packageNode = api.getPackageNode(packageElement.getQualifiedName().toString());
        TypeNode returnType = new TypeNode(qualifiedTypeName, simpleName, packageNode);
        returnType.setArrayBrackets(arrayBrackets);
        MethodNode methodNode = new MethodNode(returnType, element.getSimpleName().toString());

        TypeElement ownerElement = getEnclosingTypeElement(element);
        TypeNode ownerType = api.getTypeNode(ownerElement);
        setMethodParams(methodNode, element);

        setModifiers(methodNode, element.getModifiers());
        methodNode.setThrownTypes(element.getThrownTypes());
        methodNode.setOwner(ownerType);

        if (element.getKind() == ElementKind.METHOD) {
            MethodNode existingMethodNode = ownerType.getMethod(methodNode);
            if (existingMethodNode == null) {   
                ownerType.getMethods().add(methodNode);
            }
        }else if (element.getKind() == ElementKind.CONSTRUCTOR) {
            methodNode.setSimpleName(ownerType.getSimpleName());
            MethodNode existingMethodNode = ownerType.getConstructor(methodNode);
            if (existingMethodNode == null) {
                ownerType.getConstructors().add(methodNode);
            }
        }
        setMethodAnnotations(methodNode, element);
        setSpecifiedBy(methodNode, element);
        return methodNode;
    }

    public static FieldNode nodeFromElement(VariableElement element) {
        TypeElement classElement = getEnclosingTypeElement(element);
        if (classElement == null) {
            Configuration.getReporter().print(Diagnostic.Kind.ERROR, "No enclosing type for " + element.getSimpleName().toString());
            return null;
        }
        TypeNode typeNode = api.getTypeNode(classElement);
        if (typeNode != null) {
            String simpleName = element.getSimpleName().toString();
            FieldNode fieldNode = typeNode.getField(simpleName);
            if (fieldNode == null) {
                String qualifiedClassName = classElement.getQualifiedName().toString();
                TypeNode type = getFieldType(environment.getElementUtils(), qualifiedClassName, simpleName);
                fieldNode = new FieldNode(type, simpleName);
                typeNode.getFields().add(fieldNode);
            }
            return fieldNode;
        }
        return null;
    }

    public static TypeNode createTypeNode(String qualifiedName, String simpleName, PackageNode packageNode, ElementKind elementKind) {
        switch(elementKind) {
            case ElementKind.CLASS:
                return new ClassNode(qualifiedName, simpleName, packageNode);
            case ElementKind.INTERFACE:
                return new InterfaceNode(qualifiedName, simpleName, packageNode);
            case ElementKind.ENUM:
                return new EnumNode(qualifiedName, simpleName, packageNode);
            case ElementKind.ANNOTATION_TYPE:
                return new AnnotationNode(qualifiedName, simpleName, packageNode);
            default:
                return null;            
        }
    }

    public static Text createText(List<? extends DocTree> dtList) {
        Text text = Text.empty();
        for (DocTree docTree : dtList) {
            text.append(createTextSegment(docTree));
        }
        return text;
    }

    public static Text.Segment createTextSegment(DocTree docTree) {
        Text.Segment segment = Text.Segment.empty();
        switch(docTree.getKind()) {
            case MARKDOWN:
                segment.setKind(SegmentKind.MARKDOWN);
                segment.setText(docTree.toString());
                break;
            case TEXT:
                segment.setKind(SegmentKind.TEXT);
                segment.setText(docTree.toString());
                break;
            case LINK, LINK_PLAIN:
                segment.setKind(SegmentKind.LINK);
                segment.setLink(getDocTreePart(docTree, 1));
                break;
            case CODE:
                segment.setKind(SegmentKind.CODE);
                segment.setText(getDocTreeText(docTree, 1));
                break;
            case START_ELEMENT:
                segment.setKind(SegmentKind.START);
                StartElementTree se = (StartElementTree)docTree;
                if ("p".equals(se.getName().toString())) {
                    segment.setText("\n\n");
                }
                break;
            case END_ELEMENT:
                segment.setKind(SegmentKind.END);
                break;
            default:
                break;
        }
        return segment;
    }

    public static String getDocTreeText(DocTree docTree, int start) {
        String input = docTree.toString();
        String[] parts = input.split(" ");
        if (parts.length >= start) {
            parts[parts.length-1] = parts[parts.length-1].substring(0, parts[parts.length-1].length() - 1);
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i<parts.length; i++) {
                if (i > 1) {
                    sb.append(" ");
                }
                sb.append(parts[i]);
            }
            return sb.toString();
        }
        return "";
    }

    public static String getDocTreePart(DocTree docTree, int n) {
        String input = docTree.toString();
        String[] parts = input.split(" ");
        if (n < parts.length) {
            parts[parts.length-1] = parts[parts.length-1].substring(0, parts[parts.length-1].length() - 1);
            return parts[n];
        }
        return "";
    }

    public static void setDocumentation(Node node, Element e) {
        DocCommentTree dct = environment.getDocTrees().getDocCommentTree(e);
        if (dct != null) {
            node.setFirstSentence(createText(dct.getFirstSentence()));
            node.setBody(createText(dct.getBody()));
            node.setFullBody(createText(dct.getFullBody()));
        }
    }
        
    public static void setTypeOwnership(TypeNode typeNode, TypeElement element) {
        if (typeNode == null) return;
        TypeElement owner = getEnclosingTypeElement(element);
        TypeNode ownerTypeNode = owner == null ? null : api.getTypeNode(owner);
        if (ownerTypeNode != null) {
            // Owner is a type (class, interface, enum, annotation)
            typeNode.setOwner(ownerTypeNode);
            typeNode.setSimpleName(ownerTypeNode.getSimpleName() + "." + typeNode.getSimpleName());
        } else {
            // Owner is a package
            typeNode.setOwner(typeNode.getPackage());
        }
        typeNode.getOwner().addType(typeNode);
    }

    public static void setMethodAnnotations(MethodNode method, ExecutableElement methodElement) {
        for (AnnotationMirror anno : methodElement.getAnnotationMirrors()) {
            DeclaredType declaredType = anno.getAnnotationType();
            Element typeElement = declaredType.asElement();
            if ("Override".equals(typeElement.getSimpleName().toString())) {
                OverriddenMethodNode overrides = getOverriddenMethod(method, methodElement);
                method.setOverriddenMethod(overrides);
            }
        }
    }

    public static void setSpecifiedBy(MethodNode methodNode, ExecutableElement methodElement) {
        List<String> interfaces = methodNode.getOwner().getImplementedInterfaces();
        if (interfaces == null || interfaces.isEmpty()) return;
        for (String interfaceName : interfaces) {
            TypeElement interfaceElement = environment.getElementUtils().getTypeElement(interfaceName);
            if (interfaceElement == null) continue;
            for (ExecutableElement interfaceMethod : ElementFilter.methodsIn(interfaceElement.getEnclosedElements())) {
                if (interfaceMethod.getSimpleName().equals(methodElement.getSimpleName())) {
                    methodNode.setSpecifiedBy(interfaceName);
                    return;
                }
            }
        }
    }

    public static OverriddenMethodNode getOverriddenMethod(MethodNode method, ExecutableElement methodElement) {
        if (method.getOwner() == null) return null;

        for (int i=method.getOwner().getSupertypes().size() - 1; i>=0; i--) {
            String typeName = method.getOwner().getSupertypes().get(i);
            TypeElement superclass = environment.getElementUtils().getTypeElement(typeName);
            OverriddenMethodNode overridden = getOverriddenMethod(superclass, methodElement);
            if (overridden != null) return overridden;
            overridden = getOverriddenNativeMethod(typeName, method);
            if (overridden != null) return overridden;
        }
        return null;
    }

    public static OverriddenMethodNode getOverriddenMethod(TypeElement superclass, ExecutableElement methodElement) {
        if (superclass == null) return null;
        for (Element superMethod : superclass.getEnclosedElements()) {
            if (superMethod instanceof ExecutableElement && superMethod.getSimpleName().equals(methodElement.getSimpleName())) {
                return new OverriddenMethodNode(superclass.getQualifiedName().toString(), superMethod.getSimpleName().toString());
            }
        }
        return null;
    }

    public static OverriddenMethodNode getOverriddenNativeMethod(String qualifiedTypeName, MethodNode method) {
        try {
            String canonicalName = Utils.removeGenerics(qualifiedTypeName);
            Class<?> cls = Class.forName(canonicalName);
            Method[] listMethods = cls.getDeclaredMethods();
            for (Method listMethod : listMethods) {
                // Compare method names and parameter types
                if (listMethod.getName().equals(method.getSimpleName()) &&
                    listMethod.getParameterCount() == method.getParams().size()) {
                    boolean parametersMatch = true;
                    Class<?>[] listMethodParamTypes = listMethod.getParameterTypes();
                    for (int i = 0; i < listMethodParamTypes.length; i++) {
                        Class<?> clsB = Class.forName(method.getParams().get(i).getType().getQualifiedName());
                        if (!listMethodParamTypes[i].isAssignableFrom(clsB)) {
                            parametersMatch = false;
                            break;
                        }
                    }
                    if (parametersMatch) {
                        return new OverriddenMethodNode(qualifiedTypeName, listMethod.getName());
                    }
                }
            }
        } catch (SecurityException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null; // No overridden method found
    }

    public static void addConstantFieldValuesReference(ModuleNode moduleNode) {
        for (PackageMember member : api.getClasses()) {
            if (member instanceof ClassNode classNode) {
                for (FieldNode fieldNode : classNode.getFields()) {
                    if (fieldNode.getConstantValue() != null) {
                        Reference ref = new Reference(Reference.Kind.PAGE, "Constant Field Values", "constant-values.md");
                        fieldNode.getReferences().add(ref);
                        moduleNode.addConstantValue(fieldNode);
                    }
                }
            }
        }
    }

    public static boolean isInterface(TypeMirror typeMirror) {
        Element element = environment.getTypeUtils().asElement(typeMirror);
        if (element == null) return false;
        if (element instanceof TypeElement elem) {
            ElementKind kind = elem.getKind();
            return kind.isInterface();
        }
        return false;
    }

    public static void findImplementedInterfaces(TypeElement typeElement, List<String> result) {
        List<? extends TypeMirror> interfaces = typeElement.getInterfaces();
        for (TypeMirror interfaceType : interfaces) {
            result.add(interfaceType.toString());
        }
    }

    public static void collectAllSupertypes(TypeMirror t, List<String> result) {
        for (TypeMirror s : environment.getTypeUtils().directSupertypes(t)) {
            if (result != null) {
                String name = s.toString();
                if (!"java.lang.Object".equals(name)) {
                    if (!isInterface(s)){
                        result.add(0, name);
                    }
                    collectAllSupertypes(s, result);
                }
            }
        }
    }

    public static DeprecatedTree getDeprecation(DocCommentTree docComment) {
        if (docComment == null) return null;
        for (DocTree docTree : docComment.getBlockTags()) {
            if (docTree instanceof DeprecatedTree tree) {
                return tree;
            }
        }
        return null;
    }        

    public static ReturnTree getReturnTree(DocCommentTree dcTree) {
        if (dcTree == null) return null;
        for (DocTree docTree : dcTree.getBlockTags()) {
            if (docTree instanceof ReturnTree tree) {
                return tree;
            }
        }
        return null;
    }        

    public static ParamTree getParamTree(DocCommentTree dcTree, VariableElement parameter) {
        if (dcTree == null) return null;
        for (DocTree tagTree : dcTree.getBlockTags()) {
            if (tagTree instanceof ParamTree tree && tree.getName().toString().equals(parameter.getSimpleName().toString())) {
                return tree;
            }
        }
        return null;
    }

    public static List<Reference> getReferences(DocCommentTree dcTree) {
        if (dcTree == null) return new ArrayList<>();
        List<Reference> refs = new ArrayList<>();
        for (DocTree tagTree : dcTree.getBlockTags()) {
            if (tagTree instanceof SeeTree seeTree) {
                List<? extends DocTree> see = seeTree.getReference();
                for (DocTree docRef : see) {
                    if (docRef.getKind() == Kind.MARKDOWN) {
                        // It's actually HTML, not Markdown.
                        Reference ref = new Reference();
                        ref.setKind(Reference.Kind.URL);
                        ref.setUri(getUrl(docRef.toString()));
                        refs.add(ref);
                    } else if (docRef.getKind() == Kind.REFERENCE) {
                        Reference ref = new Reference();
                        ref.setKind(Reference.Kind.TYPE);
                        ref.setName(docRef.toString());
                        refs.add(ref);
                    } else {
                        Configuration.getReporter().print(Diagnostic.Kind.WARNING, "Unhandled reference type: " + docRef.getKind().toString());
                    }
                }
            } else if (tagTree instanceof ErroneousTree) {
                Configuration.getReporter().print(Diagnostic.Kind.WARNING, "Erroneous tag: " + tagTree.toString());
            }
        }
        return refs;
    }

    public static Text getSince(DocCommentTree dcTree) {
        if (dcTree == null) return null;
        for (DocTree tagTree : dcTree.getBlockTags()) {
            if (tagTree instanceof SinceTree sinceTree) {
                return createText(sinceTree.getBody());
            } else if (tagTree instanceof ErroneousTree) {
                Configuration.getReporter().print(Diagnostic.Kind.WARNING, "Erroneous tag: " + tagTree.toString());
            }
        }
        return Text.empty();
    }

    public static String getUrl(String html) {
        if (html == null) return null;
        int start = -1;
        int end = -1;
        while (++end < html.length()) {
            if (html.charAt(end) == '"') {
                if (start == -1) {
                    start = end;
                } else {
                    return html.substring(start + 1, end);
                }
            }
        }
        return null;
    }

    public static void setModifiers(Node node, Set<Modifier> modifiers) {
        for (Modifier modifier : modifiers) {
            io.github.sandydunlop.markista.model.Modifier mod = 
                io.github.sandydunlop.markista.model.Modifier.valueOf(modifier.name());
            node.addModifier(mod);
        }
    }

    public static void setDeprecationStatus(Node node, Element e, DocCommentTree dct) {
        if (node == null) return;
        DeprecatedTree deprecatedTree = getDeprecation(dct);
        Deprecated deprecatedAnnotation = e.getAnnotation(Deprecated.class);
        node.setDeprecation(Deprecation.NONE);
        if (deprecatedTree != null) {
            node.setDeprecation(Deprecation.DEPRECATED);
            node.setDeprecationText(createText(deprecatedTree.getBody()));
        }
        if (deprecatedAnnotation != null) {
            if (deprecatedAnnotation.forRemoval()) {
                node.setDeprecation(Deprecation.FOR_REMOVAL);
            }else{
                node.setDeprecation(Deprecation.DEPRECATED);
            }
        }
    }

    public static boolean isIncludedInApi(Element e) {
        Set<Modifier> mods = e.getModifiers();
        return Configuration.getDocumentPrivateMembers() || mods.contains(PUBLIC) || mods.contains(PROTECTED);
    }

    public static void setMethodParams(MethodNode methodDoc, ExecutableElement ee) {
        methodDoc.getParams().clear();
        DocCommentTree dct = environment.getDocTrees().getDocCommentTree(ee);
        for (VariableElement parameter : ee.getParameters()) {
            String simpleName = parameter.getSimpleName().toString();
            TypeNode paramType = getParamType(ee, simpleName);
            ParamNode param = new ParamNode(paramType, simpleName);
            ParamTree paramTree = getParamTree(dct, parameter);
            if (paramTree != null) {
                param.setBody(createText(paramTree.getDescription()));
            }
            methodDoc.addParam(param);
        }
    }

    public static TypeNode getFieldType(Elements elementUtils, String className, String fieldName) {
        String qualifiedTypeName = null;
        String arrayBrackets = "";
        TypeElement classElement = elementUtils.getTypeElement(className);
        for (VariableElement field : ElementFilter.fieldsIn(classElement.getEnclosedElements())) {
            if (field.getSimpleName().toString().equals(fieldName)) {
                TypeMirror typeMirror = field.asType();
                if (typeMirror.getKind() == TypeKind.ARRAY) {
                    TypeMirror array = ((ArrayType)typeMirror).getComponentType();
                    qualifiedTypeName = array.toString();
                    arrayBrackets = "\\[]";                    
                } else {
                    qualifiedTypeName = typeMirror.toString();
                }
                break;
            }
        }
        if (qualifiedTypeName == null) return null;

        String simpleTypeName = Utils.simplifyNames(qualifiedTypeName);
        String packageName = getPackageName(qualifiedTypeName);
        PackageNode packageNode = api.getPackageNode(packageName);
        TypeNode type = new TypeNode(qualifiedTypeName, simpleTypeName, packageNode);
        type.setArrayBrackets(arrayBrackets);

        return type;
    }

    public static TypeNode getParamType(ExecutableElement method, String fieldName) {
        String qualifiedTypeName = null;
        String arrayBrackets = "";
        for (VariableElement param : method.getParameters()) {
            if (param.getSimpleName().toString().equals(fieldName)){
                TypeMirror typeMirror = param.asType();
                if (typeMirror.getKind() == TypeKind.ARRAY){
                    TypeMirror array = ((ArrayType)typeMirror).getComponentType();
                    qualifiedTypeName = array.toString();
                    arrayBrackets = "\\[]";
                } else {
                    qualifiedTypeName = typeMirror.toString();
                }
                break;
            }
        }
        if (qualifiedTypeName == null) return null;

        String simpleTypeName = Utils.simplifyNames(qualifiedTypeName);
        String packageName = getPackageName(qualifiedTypeName);
        PackageNode packageNode = api.getPackageNode(packageName);
        TypeNode type = new TypeNode(qualifiedTypeName, simpleTypeName, packageNode);
        type.setArrayBrackets(arrayBrackets);

        return type;
    }

    private static String getPackageName(String qualifiedTypeName) {
        if (qualifiedTypeName == null) return null;
        String packageName = qualifiedTypeName;
        if (packageName.indexOf(".")>-1) {
            packageName = packageName.substring(0, packageName.lastIndexOf("."));
        }
        return packageName;
    }

    public static PackageElement getEnclosingPackageElement(Element element) {
        Element enclosing = element.getEnclosingElement();
        if (enclosing == null) return null;
        if (enclosing.getKind() == ElementKind.PACKAGE ) {
            return (PackageElement)enclosing;
        }else{
            return getEnclosingPackageElement(enclosing);
        }
    }

    /// Returns the TypeElement of the class the specified element belongs to.
    /// @param element A program element such as a field or method
    /// @return the TypeElement of the class the specified element belongs to.
    public static TypeElement getEnclosingTypeElement(Element element) {
        Element enclosing = element.getEnclosingElement();
        if (enclosing == null) {
            return null;
        }
        if (enclosing.getKind() == ElementKind.CLASS ||
                    enclosing.getKind() == ElementKind.INTERFACE ||
                    enclosing.getKind() == ElementKind.ENUM ||
                    enclosing.getKind() == ElementKind.ANNOTATION_TYPE) {
            return (TypeElement)enclosing;
        }else{
            return getEnclosingTypeElement(enclosing);
        }
    }
}
