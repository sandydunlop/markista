package io.github.sandydunlop.markista.util;

import io.github.sandydunlop.markista.core.Configuration;
import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.AbstractMember;
import io.github.sandydunlop.markista.model.AnnotationElement;
import io.github.sandydunlop.markista.model.AnnotationTypeNode;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.AppliedAnnotationNode;
import io.github.sandydunlop.markista.model.ClassTypeNode;
import io.github.sandydunlop.markista.model.Deprecation;
import io.github.sandydunlop.markista.model.DirectiveNode;
import io.github.sandydunlop.markista.model.EnumTypeNode;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.InterfaceTypeNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.Node;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.Pair;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.RecordTypeNode;
import io.github.sandydunlop.markista.model.Reference;
import io.github.sandydunlop.markista.model.Text;
import io.github.sandydunlop.markista.model.Text.Segment;
import io.github.sandydunlop.markista.model.Text.SegmentKind;
import io.github.sandydunlop.markista.model.TypeNode;
import io.github.sandydunlop.markista.model.TypeView;
import io.github.sandydunlop.markista.util.MarkdownParser.TokenKind;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.JavaFileObject;

import jdk.javadoc.doclet.DocletEnvironment;

import com.sun.source.doctree.DeprecatedTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTree.Kind;
import com.sun.source.doctree.ErroneousTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.doctree.SinceTree;
import com.sun.source.doctree.StartElementTree;

import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;

/// Utility class providing static methods to create and manipulate TypeNodes, MethodNodes, FieldNodes, and other API model objects
/// from language model elements and Javadoc doc trees obtained from the Java source code.
/// 
/// This class bridges the Java language model and the internal API representation used for generating documentation.
/// It includes methods to extract element details, ownership, modifiers, supertypes, interfaces, annotations, and documentation text.
/// 
/// The TypeUtils class must be initialized with an Api and DocletEnvironment before usage via the init(Api, DocletEnvironment) method.
/// 
/// It provides numerous helper methods to process types, methods, fields, annotations, and project structure metadata.
/// 
/// Methods also support handling Javadoc comment trees to extract detailed documentation fragments such as `@deprecated`, @param, @return, @since, and @see tags.
/// 
/// The class works internally with the Api model for cross-referencing and linking discovered elements.
/// 
/// This class is for internal use within the documentation generator and is not thread-safe.
@SuppressWarnings({"squid:S1123", "squid:S1133"}) // Sonar thinks this is deprecated, but it's not.
public class TypeUtils {
    /// The Context singleton instance providing access to the current documentation generation context,
    /// including configuration, current module/package/type names, and reporting utilities.
    static Context ctx;

    private static DocletEnvironment environment;

    static ExecutableElement currentMethodElement;
    static MethodNode currentMethodNode;

    /// The Api model representing the entire documented API structure,
    /// including modules, packages, types, and members used for cross-referencing and navigation.
    private static Api api;

    private TypeUtils() {
        // Hide public constructor
    }

    /// Initializes this utility class to use the given API model and Doclet environment.
    /// Sets up internal references and context necessary for subsequent operations.
    /// @param a The Api instance representing the overall API model.
    /// @param e The DocletEnvironment providing access to Javadoc doc trees and processing utilities.
    public static void init(Api a, DocletEnvironment e) {
        api = a;
        environment = e;
        ctx = Context.getInstance();
    }

    /// Creates or retrieves a TypeNode from the supplied TypeElement.
    /// This method extracts type details such as qualified name, package, kind, ownership, modifiers, supertypes, and interfaces,
    /// adds the new TypeNode to the API model, and returns it.
    /// @param element The language model TypeElement to create a TypeNode from.
    /// @return The corresponding TypeNode in the API model, or null if unsupported or error occurs.
    public static TypeNode nodeFromElement(TypeElement element) {
        String qualifiedName = element.getQualifiedName().toString();
        TypeNode typeNode = api.getTypeNode(qualifiedName);
        if (typeNode == null) {
            ctx.setTypeName(element.getQualifiedName().toString());
            if (Configuration.getVerbose()) {
                ctx.reportInfo(String.format("[   TYPE] %s", qualifiedName));
            }
            PackageElement packageElement = getEnclosingPackageElement(element);
            String simpleName = element.getSimpleName().toString();
            if (packageElement == null) {
                ctx.reportError("No package for " + qualifiedName);
                return null;
            }
            PackageNode packageNode = api.getPackageNode(packageElement.getQualifiedName().toString());
            typeNode = createTypeNode(qualifiedName, simpleName, packageNode, element.getKind());
            if (typeNode == null) {
                ctx.reportError("Unsupported type kind: " + element.getKind());
                return null;
            }
            if (typeNode instanceof EnumTypeNode enumNode) {
                setEnumConstants(enumNode, element);
            }
            setTypeOwnership(typeNode, element);
            setModifiers(typeNode, element.getModifiers());
            setAppliedAnnotations(typeNode, element);
            collectAllSupertypes(element.asType(), typeNode.getSupertypes());
            typeNode.getSupertypes().addFirst(Pair.of(Reference.to("java.lang.Object"), Text.empty()));
            findImplementedInterfaces(element, typeNode.getImplementedInterfaces());
            setDocumentation(typeNode, element);
            setSourcePath(typeNode, element);
            api.addType(typeNode);
        }
        return typeNode;
    }

    /// Reads enum constants from the TypeElement and adds them as FieldNodes to the EnumNode.
    /// @param enumNode The EnumNode to populate with constants.
    /// @param e The TypeElement representing the enum type.
    public static void setEnumConstants(EnumTypeNode enumNode, TypeElement e) {
        List<? extends Element> enclosedElements = e.getEnclosedElements();
        for (Element element : enclosedElements) {
            if (element.getKind() == ElementKind.ENUM_CONSTANT) {
                FieldNode enumConstant = new FieldNode(null, element.getSimpleName().toString());
                enumNode.getConstants().add(enumConstant);
            }
        }
    }

    /// Creates a MethodNode representation from the ExecutableElement element (method or constructor).
    /// Sets return type, parameters, modifiers, thrown exceptions, ownership, and annotations.
    /// Adds the method to the owning TypeNode's method or constructor list.
    /// @param element The ExecutableElement to convert.
    /// @return The constructed MethodNode, or null if errors occur.
    public static MethodNode nodeFromElement(ExecutableElement element) {
        TypeMirror typeMirror = element.getReturnType();
        String qualifiedTypeName = typeMirror.toString();
        String arrayBrackets = "";
        if (typeMirror.getKind() == TypeKind.ARRAY) {
            TypeMirror array = ((ArrayType)typeMirror).getComponentType();
            qualifiedTypeName = array.toString();
            arrayBrackets = "\\[]";                    
        }
        PackageElement packageElement = getEnclosingPackageElement(element);
        if (packageElement == null) {
            ctx.reportError("No package for " + qualifiedTypeName);
            return null;
        }
        String returnTypeName = qualifiedTypeName + arrayBrackets;
        MethodNode methodNode = new MethodNode(returnTypeName, element.getSimpleName().toString());
        PackageNode packageNode = api.getPackageNode(packageElement.getQualifiedName().toString());

        // setMethodParams must be called before setMethodOwnerDetails as the method
        // parameters need to be present to determine if this method already exists.
        setMethodParams(methodNode, element);
        if (!setMethodOwnerDetails(methodNode, packageNode, element)) {
            return null;
        }

        currentMethodElement = element; // Used for @inheritDoc
        currentMethodNode = methodNode;

        setModifiers(methodNode, element.getModifiers());
        setThrownTypes(methodNode, element.getThrownTypes());
        setMethodAnnotations(methodNode, element);
        setAppliedAnnotations(methodNode, element);
        setSpecifiedBy(methodNode, element);
        DocCommentTree dct = environment.getDocTrees().getDocCommentTree(element);
        TypeUtils.setDeprecationStatus(methodNode, element, dct);
        if (dct != null) {
            methodNode.setFirstSentence(TypeUtils.createText(dct.getFirstSentence()));
            methodNode.setBody(TypeUtils.createText(dct.getBody()));
            methodNode.setFullBody(TypeUtils.createText(dct.getFullBody()));
            ReturnTree returnTree = TypeUtils.getReturnTree(dct);
            if (returnTree != null) {
                methodNode.setReturnDescription(TypeUtils.createText(returnTree.getDescription()));
            }
            methodNode.setReferences(TypeUtils.getReferences(dct));
            methodNode.setSince(TypeUtils.getSince(dct));
        }
        return methodNode;
    }

    /// Creates a FieldNode representation from the VariableElement element representing a field.
    /// Links the field to the owning TypeNode.
    /// @param element The VariableElement to convert.
    /// @return The FieldNode, or null if errors occur.
    public static FieldNode nodeFromElement(VariableElement element) {
        TypeElement classElement = getEnclosingTypeElement(element);
        if (classElement == null) {
            ctx.reportError("No enclosing type for " + element.getSimpleName().toString());
            return null;
        }
        TypeNode typeNode = api.getTypeNode(classElement.getQualifiedName().toString());
        if (typeNode != null) {
            String simpleName = element.getSimpleName().toString();
            FieldNode fieldNode = typeNode.getField(simpleName);
            if (fieldNode == null) {
                fieldNode = new FieldNode(element.asType().toString(), simpleName);
                typeNode.addField(fieldNode);
                fieldNode.setConstantValue((Serializable) element.getConstantValue());
                DocCommentTree dct = environment.getDocTrees().getDocCommentTree(element);
                setDocumentation(fieldNode, element);
                setModifiers(fieldNode, element.getModifiers());
                setDeprecationStatus(fieldNode, element, dct);
                setAppliedAnnotations(fieldNode, element);
            }
            return fieldNode;
        }
        return null;
    }

    /// Factory method to create TypeNode (ClassTypeNode, InterfaceTypeNode, RecordTypeNode,
    /// EnumTypeNode, or AnnotationTypeNode) based on ElementKind.
    /// @param qualifiedName Fully qualified name of the type.
    /// @param simpleName The simple (unqualified) name of the type.
    /// @param packageNode The owning PackageNode.
    /// @param elementKind The ElementKind representing the type kind.
    /// @return A TypeNode instance corresponding to the kind, or null if unsupported.
    public static TypeNode createTypeNode(String qualifiedName, String simpleName, PackageNode packageNode, ElementKind elementKind) {
        return switch (elementKind) {
            case ElementKind.CLASS -> new ClassTypeNode(qualifiedName, simpleName, packageNode.getQualifiedName());
            case ElementKind.INTERFACE -> new InterfaceTypeNode(qualifiedName, simpleName, packageNode
                    .getQualifiedName());
            case ElementKind.RECORD -> new RecordTypeNode(qualifiedName, simpleName, packageNode.getQualifiedName());
            case ElementKind.ENUM -> new EnumTypeNode(qualifiedName, simpleName, packageNode.getQualifiedName());
            case ElementKind.ANNOTATION_TYPE -> new AnnotationTypeNode(qualifiedName, simpleName, packageNode
                    .getQualifiedName());
            default -> null;
        };
    }

    /// Creates a complete Text object by traversing a list of DocTree nodes from the Javadoc comment.
    /// @param dtList List of DocTree nodes representing a part of a Javadoc comment.
    /// @return A Text object composed of segments derived from each DocTree node.
    public static Text createText(List<? extends DocTree> dtList) {
        Text text = Text.empty();
        for (DocTree docTree : dtList) {
            text.append(docTreeToText(docTree));
        }
        return text;
    }

    /// Creates a [Text] object from a DocTree node, setting the appropriate kind and content.
    /// @param docTree The DocTree node to convert.
    /// @return A [Text] object representing the content and kind of the provided DocTree.
    public static Text docTreeToText(DocTree docTree) {
        Text text = Text.empty();
        Text.Segment segment = Text.Segment.empty();
        String origin;
        switch(docTree.getKind()) {
            case MARKDOWN:
                text = markdownToText(docTree.toString());
                break;
            case TEXT:
                text.append(docTree.toString());
                break;
            case LINK:
                segment.setKind(SegmentKind.LINK);
                origin = ctx.getTypeName().isEmpty() ? ctx.getPackageName() : ctx.getTypeName();
                Reference link = Reference.to(getDocTreePart(docTree, 1)).from(origin);
                api.addLink(link);
                segment.setLink(link);
                text.append(segment);
                break;
            case LINK_PLAIN:
                segment.setKind(SegmentKind.LINK);
                origin = ctx.getTypeName().isEmpty() ? ctx.getPackageName() : ctx.getTypeName();
                link = Reference.to(getDocTreePart(docTree, 1)).from(origin);
                segment.setLink(link);
                text.append(segment);
                api.addLink(link);
                if (docTree instanceof LinkTree linkTree) {
                    segment.setText(createText(linkTree.getLabel()).toString());
                    link.setLabel(createText(linkTree.getLabel()).toString());
                }
                break;
            case CODE:
                segment.setKind(SegmentKind.CODE);
                segment.setText(getDocTreeText(docTree, 1));
                text.append(segment);
                break;
            case START_ELEMENT:
                segment.setKind(SegmentKind.TEXT);
                StartElementTree se = (StartElementTree)docTree;
                if ("p".equals(se.getName().toString())) {
                    segment.setText("\n\n");
                }
                text.append(segment);
                break;
            case END_ELEMENT:
                break;
            case INHERIT_DOC:
                // Inherited docs are processed in TextAssembler as they need the
                // full API model which is incomplete here.
                segment.setKind(SegmentKind.INHERIT);
                text.append(segment);
                break;
            default:
                break;
        }
        return text;
    }

    /// Converts Markdown text into a [Text] object
    /// @param markdown Markdown formatted text possibly containing links
    /// @return [Text] version of the Markdown
    public static Text markdownToText(String markdown) {
        Text text = Text.empty();
        MarkdownParser parser = new MarkdownParser(markdown);
        MarkdownParser.Token token = parser.firstToken();
        while (token.getKind() != MarkdownParser.TokenKind.END) {
            if (token.getKind() == MarkdownParser.TokenKind.BRACKETS_TAG) {
                MarkdownParser.Token next = token.getNext();
                if (next.getKind() == TokenKind.BRACKETS_TAG || next.getKind() == TokenKind.PARENS_TAG) {
                    Reference ref = Reference.to(next.getText());
                    ref.setLabel(token.getText());
                    Segment segment = Segment.empty()
                            .setKind(SegmentKind.LINK)
                            .setLink(ref)
                            .setText(token.getText());
                    text.append(segment);
                    api.addLink(ref);
                    token = next;
                } else {
                    Reference ref = Reference.to(token.getText());
                    Segment segment = Segment.empty()
                            .setKind(SegmentKind.LINK)
                            .setLink(ref)
                            .setText(token.getText());
                    text.append(segment);
                    api.addLink(ref);
                }
            } else if (token.getKind() == TokenKind.TEXT) {
                text.append(token.getText());
            }
            token = token.getNext();
        }
        return text;
    }

    /// Extracts text from a DocTree to build a string from a part of its tokenized representation.
    /// @param docTree The DocTree to extract from.
    /// @param start The starting index for extraction.
    /// @return A string representing the extracted part or empty string if extraction fails.
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

    /// Extracts a specific part (token) from a DocTree's toString representation.
    /// @param docTree The DocTree to parse.
    /// @param n The zero-based index of the part to extract.
    /// @return The extracted string part or empty string if out of range.
    public static String getDocTreePart(DocTree docTree, int n) {
        String input = docTree.toString();
        String[] parts = input.split(" ");
        if (n < parts.length) {
            parts[parts.length-1] = parts[parts.length-1].substring(0, parts[parts.length-1].length() - 1);
            return parts[n];
        }
        return "";
    }

    public static boolean setMethodOwnerDetails(MethodNode methodNode, PackageNode packageNode, ExecutableElement element) {
        TypeElement ownerElement = getEnclosingTypeElement(element);
        if (ownerElement != null) {
            TypeNode ownerType = api.getTypeNode(ownerElement.getQualifiedName().toString());
            if (ownerType == null) {
                ctx.reportError(String.format(
                        "Unable to determine owner of method '%s' in package '%s'",
                        methodNode.getSimpleName(), packageNode.getQualifiedName()));
                return false;
            }
            methodNode.setOwnerName(ownerType.getQualifiedName());
            if (element.getKind() == ElementKind.METHOD) {
                MethodNode existingMethodNode = ownerType.getMethod(methodNode);
                if (existingMethodNode == null) {   
                    ownerType.getMethods().add(methodNode);
                } else {
                    // Method already exists in the API model.
                    // Returning false instructs the caller not to continue.
                    return false;
                }
            } else if (element.getKind() == ElementKind.CONSTRUCTOR) {
                methodNode.setSimpleName(ownerType.getSimpleName());
                MethodNode existingMethodNode = ownerType.getConstructor(methodNode);
                if (existingMethodNode == null) {
                    ownerType.addConstructor(methodNode);
                } else {
                    // Method already exists in the API model
                    // Returning false instructs the caller not to continue.
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public static void setSourcePath(TypeNode typeNode, TypeElement element) {
        JavaFileObject jfo = environment.getElementUtils().getFileObjectOf(element);
        if (jfo != null) {
            typeNode.setSourcePath(Path.of(jfo.toUri()).toString());
            PackageNode typePackage = api.getPackageNode(typeNode.getPackageName());
            if (typePackage != null && typePackage.getSourcePath() == null) {
                typePackage.setSourcePath(Path.of(jfo.toUri()).getParent().toString());
            }
        }
    }

    /// Sets documentation text for a Node based on the doc comment tree attached to a language model element.
    /// This populates first sentence, body, and full body texts.
    /// @param node The Node to set documentation for.
    /// @param e The element whose doc comment is used.
    public static void setDocumentation(Node node, Element e) {
        DocCommentTree dct = environment.getDocTrees().getDocCommentTree(e);
        if (dct != null) {
            node.setFirstSentence(createText(dct.getFirstSentence()));
            node.setBody(createText(dct.getBody()));
            node.setFullBody(createText(dct.getFullBody()));
        }
    }

    /// Sets ownership of a TypeNode based on its enclosing type or package.
    /// Updates the ownership link and qualified names accordingly.
    /// @param typeNode The TypeNode to set ownership on.
    /// @param element The TypeElement representing the type.
    public static void setTypeOwnership(TypeNode typeNode, TypeElement element) {
        if (typeNode == null) return;
        TypeElement owner = getEnclosingTypeElement(element);
        TypeNode ownerTypeNode = owner == null ? null : api.getTypeNode(owner.getQualifiedName().toString());
        if (ownerTypeNode != null) {
            // Owner is a type (class, interface, enum, annotation)
            typeNode.setOwner(ownerTypeNode.getQualifiedName());
            typeNode.setSimpleName(ownerTypeNode.getSimpleName() + "." + typeNode.getSimpleName());
            ownerTypeNode.addType(typeNode);
        } else {
            // Owner is a package
            PackageNode ownerPackage = api.getPackageNode(typeNode.getPackageName());
            typeNode.setOwner(ownerPackage.getQualifiedName());
            ownerPackage.addType(typeNode);
        }
    }

    /// Sets annotations on the MethodNode, in particular looks for @Override annotation to set overridden methods.
    /// @param method The MethodNode to update.
    /// @param methodElement The ExecutableElement representing the method.
    public static void setMethodAnnotations(MethodNode method, ExecutableElement methodElement) {
        for (AnnotationMirror anno : methodElement.getAnnotationMirrors()) {
            DeclaredType declaredType = anno.getAnnotationType();
            Element typeElement = declaredType.asElement();
            if ("Override".equals(typeElement.getSimpleName().toString())) {
                String nativeBaseClass = getNativeClassForInheritedMethod(method);
                Reference overriddenMethod = null;
                if (nativeBaseClass != null) {
                    String methodName = methodElement.getSimpleName().toString();
                    overriddenMethod = Reference.to(nativeBaseClass + "#" + methodName);
                } else {
                    overriddenMethod = Reference.toMethod(getFullSignature(methodElement));
                }
                method.setBaseMethod(overriddenMethod);
            }
        }
    }

    /// Checks implemented interfaces of a method's owning type, and sets "specifiedBy" on the method if it implements an interface method.
    /// @param methodNode The MethodNode to update.
    /// @param methodElement The ExecutableElement representing the method.
    public static void setSpecifiedBy(MethodNode methodNode, ExecutableElement methodElement) {
        TypeNode ownerTypeNode = api.getTypeNode(methodNode.getOwnerName());
        List<Reference> interfaces = ownerTypeNode.getImplementedInterfaces();
        if (interfaces == null || interfaces.isEmpty()) return;
        for (Reference interfaceName : interfaces) {
            TypeElement interfaceElement = environment.getElementUtils().getTypeElement(interfaceName.getClassName());
            if (interfaceElement == null) continue;
            List<? extends Element>  enclosedElements = interfaceElement.getEnclosedElements();
            for (ExecutableElement interfaceMethod : ElementFilter.methodsIn(enclosedElements)) {
                if (interfaceMethod.getSimpleName().toString().equals(methodElement.getSimpleName().toString())) {
                    methodNode.setSpecifiedBy(interfaceName);
                    return;
                }
            }
        }
    }

    static String getFullSignature(ExecutableElement element) {
        StringBuilder signature = new StringBuilder();
        
        // Get method name
        signature.append(element.getSimpleName()).append("(");
        
        // Get parameter types
        List<? extends VariableElement> parameters = element.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            TypeMirror type = parameters.get(i).asType();
            signature.append(type.toString());
            if (i < parameters.size() - 1) {
                signature.append(", ");
            }
        }
        
        signature.append(")");
        return signature.toString();
    }

    /// Recursively searches for an overridden method matching the supplied method within the supertypes of its owner.
    /// @param method The MethodNode for which to find an overridden method.
    /// @return An InheritedMethodNode if a matching overriden method is found, else null.
    public static String getNativeClassForInheritedMethod(MethodNode method) {
        if (method.getOwnerName() == null) return null;

        TypeNode ownerTypeNode = api.getTypeNode(method.getOwnerName());
        for (int i = ownerTypeNode.getSupertypes().size() - 1; i >= 0; i--) {
            Pair<Reference, Text> pair = ownerTypeNode.getSupertypes().get(i);
            Reference typeRef = pair.getL();
            String canonicalName = Utils.removeGenerics(typeRef.getTarget());
            try {
                Class<?> cls = Class.forName(canonicalName);
                if (cls != null && belongsToClass(cls, method)) {
                    return canonicalName;
                }
            } catch (SecurityException | ClassNotFoundException _) {
                ctx.reportWarning("Failed to read information for " + canonicalName + "." + method.getSimpleName());
            }
        }
        return null;
    }

    /// Attempts to find an inherited method defined in native Java classes (e.g., from runtime classes).
    /// @param cls The class the method belongs to
    /// @param method The MethodNode that may override the inherited native method.
    /// @return An InheritedMethodNode if found, or null otherwise.
    /// @throws ClassNotFoundException 
    public static boolean belongsToClass(Class<?> cls, MethodNode method) throws ClassNotFoundException {
        Method[] listMethods = cls.getDeclaredMethods();
        for (Method listMethod : listMethods) {
            // Compare method names and parameter counts
            if (listMethod.getName().equals(method.getSimpleName()) &&
                listMethod.getParameterCount() == method.getParams().size()) {
                boolean parametersMatch = true;
                Class<?>[] listMethodParamTypes = listMethod.getParameterTypes();
                for (int i = 0; i < listMethodParamTypes.length; i++) {
                    Class<?> clsB = Class.forName(method.getParams().get(i).getTypeName());
                    if (!listMethodParamTypes[i].isAssignableFrom(clsB)) {
                        parametersMatch = false;
                        break;
                    }
                }
                if (parametersMatch) {
                    return true;
                }
            }
        }
        return false; // No overridden method found
    }

    /// Adds references to constant field values from classes in the API to the provided module node.
    /// @param moduleNode The ModuleNode to which constant value references will be added.
    public static void addConstantFieldValuesReference(ModuleNode moduleNode) {
        for (TypeView classNode : api.getTypes()) {
            for (FieldNode fieldNode : ((TypeNode)classNode).getFields()) {
                if (fieldNode.getConstantValue() != null) {
                    Reference ref = Reference.to("constant-values")
                            .from(((TypeNode) classNode).getPackageName())
                            .withKind(Reference.Kind.PAGE)
                            .withLabel("Constant Field Values");
                    fieldNode.getReferences().add(ref);
                    moduleNode.addConstantValue(fieldNode);
                }
            }
        }
    }

    /// Returns true if the TypeMirror represents an interface.
    /// @param typeMirror The TypeMirror to check.
    /// @return true if the type is an interface, false otherwise.
    public static boolean isInterface(TypeMirror typeMirror) {
        Element element = environment.getTypeUtils().asElement(typeMirror);
        if (element == null) return false;
        if (element instanceof TypeElement elem) {
            ElementKind kind = elem.getKind();
            return kind.isInterface();
        }
        return false;
    }

    /// Finds all interfaces implemented directly by the given TypeElement and adds their names to the result list.
    /// @param typeElement The type to examine.
    /// @param result The list to receive the qualified interface names.
    public static void findImplementedInterfaces(TypeElement typeElement, List<Reference> result) {
        List<? extends TypeMirror> interfaces = typeElement.getInterfaces();
        for (TypeMirror interfaceType : interfaces) {
            Reference reference = Reference
                    .to(interfaceType.toString())
                    .from(ctx.getPackageName())
                    .withKind(Reference.Kind.TYPE)
                    .withLabel(interfaceType.toString());
            result.add(reference);
        }
    }

    /// Collects all supertypes (classes) of the specified type recursively and adds them to the result list.
    /// java.lang.Object is excluded.
    /// @param t The type to examine.
    /// @param result The list to receive supertypes.
    public static void collectAllSupertypes(TypeMirror t, List<Pair<Reference, Text>> result) {
        for (TypeMirror s : environment.getTypeUtils().directSupertypes(t)) {
            if (result != null) {
                String name = s.toString();
                if (!"java.lang.Object".equals(name)) {
                    if (!isInterface(s)){
                        Reference reference = Reference.to(name)
                                .from(ctx.getPackageName())
                                .withKind(Reference.Kind.TYPE)
                                .withLabel(name);
                        result.addFirst(Pair.of(reference, Text.empty()));
                    }
                    collectAllSupertypes(s, result);
                }
            }
        }
    }

    /// Finds the DeprecatedTree from a Javadoc DocCommentTree if present.
    /// @param docComment The Javadoc comment tree.
    /// @return The DeprecatedTree if found, null otherwise.
    public static DeprecatedTree getDeprecation(DocCommentTree docComment) {
        if (docComment == null) return null;
        for (DocTree docTree : docComment.getBlockTags()) {
            if (docTree instanceof DeprecatedTree tree) {
                return tree;
            }
        }
        return null;
    }       

    /// Finds the @return tag from a Javadoc DocCommentTree if present.
    /// @param dcTree The DocCommentTree to search.
    /// @return The ReturnTree if found, null otherwise.
    public static ReturnTree getReturnTree(DocCommentTree dcTree) {
        if (dcTree == null) return null;
        for (DocTree docTree : dcTree.getBlockTags()) {
            if (docTree instanceof ReturnTree tree) {
                return tree;
            }
        }
        return null;
    }       

    /// Finds the @param tag in a DocCommentTree matching the specified parameter variable.
    /// @param dcTree The DocCommentTree containing block tags.
    /// @param parameter The VariableElement parameter to match.
    /// @return The matching ParamTree if found, null otherwise.
    public static ParamTree getParamTree(DocCommentTree dcTree, VariableElement parameter) {
        if (dcTree == null) return null;
        for (DocTree tagTree : dcTree.getBlockTags()) {
            if (tagTree instanceof ParamTree tree && tree.getName().getName().toString().equals(parameter.getSimpleName().toString())) {
                return tree;
            }
        }
        return null;
    }

    /// Extracts a list of Reference objects representing occurrences of @see tags in the Javadoc comment.
    /// @param dcTree The DocCommentTree to process.
    /// @return A list of Reference objects extracted from @see tags.
    public static List<Reference> getReferences(DocCommentTree dcTree) {
        if (dcTree == null) return new ArrayList<>();
        List<Reference> refs = new ArrayList<>();
        for (DocTree tagTree : dcTree.getBlockTags()) {
            if (tagTree instanceof SeeTree seeTree) {
                List<? extends DocTree> see = seeTree.getReference();
                for (DocTree docRef : see) {
                    if (docRef.getKind() == Kind.MARKDOWN) {
                        // This is sometimes (always?!) HTML, not Markdown?
                        refs.add(Reference.to(getUrl(docRef.toString())));
                    } else if (docRef.getKind() == Kind.REFERENCE) {
                        refs.add(Reference.to(docRef.toString()).withKind(Reference.Kind.TYPE));
                    } else {
                        ctx.reportWarning("Unhandled reference type: " + docRef.getKind().toString());
                    }
                }
            } else if (tagTree instanceof ErroneousTree) {
                ctx.reportWarning("Erroneous tag: " + tagTree);
            }
        }
        return refs;
    }

    /// Extracts the @since tag content from a DocCommentTree, if present.
    /// @param dcTree The DocCommentTree containing tags.
    /// @return A Text object representing @since content or an empty Text if none present.
    public static Text getSince(DocCommentTree dcTree) {
        if (dcTree == null) return null;
        for (DocTree tagTree : dcTree.getBlockTags()) {
            if (tagTree instanceof SinceTree sinceTree) {
                return createText(sinceTree.getBody());
            } else if (tagTree instanceof ErroneousTree) {
                ctx.reportWarning("Erroneous tag: " + tagTree);
            }
        }
        return Text.empty();
    }

    /// Extracts a URL string from html-like text, e.g., from an href attribute inside double-quotes.
    /// @param html The input HTML-like string.
    /// @return The extracted URL inside quotes or null if none found.
    public static String getUrl(String html) {
        if (html == null) return null;
        int start = -1;
        int end = -1;
        while (++end < html.length()) {
            if (html.charAt(end) == '\"') {
                if (start == -1) {
                    start = end;
                } else {
                    return html.substring(start + 1, end);
                }
            }
        }
        return null;
    }

    /// Adds modifiers to a model Node based on the set of language model modifiers.
    /// @param node The Node to add modifiers to.
    /// @param modifiers The set of Modifier enums from language model.
    public static void setModifiers(AbstractMember node, Set<Modifier> modifiers) {
        for (Modifier modifier : modifiers) {
            io.github.sandydunlop.markista.model.Modifier mod = 
                io.github.sandydunlop.markista.model.Modifier.valueOf(modifier.name());
            node.addModifier(mod);
        }
    }

    /// Adds applied annotations to a TypeNode, FieldNode, or MethodNode.
    /// @param node The TypeNode to add annotations to.
    /// @param elem The scanned Element that contains information about the applied annotations.
    public static void setAppliedAnnotations(AbstractMember node, Element elem) {
        if (elem instanceof javax.lang.model.AnnotatedConstruct annotatedConstruct) {
            List<? extends AnnotationMirror> annotationMirrors = annotatedConstruct.getAnnotationMirrors();
            for (AnnotationMirror annotationMirror : annotationMirrors) {
                setAppliedAnnotation(node, annotationMirror);
            }
        }
    }

    /// Adds an applied annotation to a TypeNode.
    /// @param node The TypeNode to add annotations to.
    /// @param annotationMirror The scanned annotation
    public static void setAppliedAnnotation(AbstractMember node, AnnotationMirror annotationMirror) {
        DeclaredType declaredType = annotationMirror.getAnnotationType();
        Element declaredElement = declaredType.asElement();
        if (declaredElement instanceof TypeElement declaredTypeElement) {
            AppliedAnnotationNode annotationNode = new AppliedAnnotationNode(
                    declaredTypeElement.getQualifiedName().toString());
            node.addAppliedAnnotation(annotationNode);
            api.addAppliedAnnotation(annotationNode);
            if (declaredTypeElement.getQualifiedName().toString().equals("java.lang.annotation.Documented")) {
                node.setHasDocumentedAnnotation(true);
            }
            Map<? extends ExecutableElement, ? extends AnnotationValue> values = annotationMirror.getElementValues();
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values.entrySet()) {
                ExecutableElement annotationMethod = entry.getKey();
                TypeMirror methodType = annotationMethod.asType();

                String typeString = methodType.toString();
                if (typeString != null && typeString.charAt(0) == '(') {
                    typeString = typeString.substring(2);
                }

                PackageElement pe = TypeUtils.getEnclosingPackageElement(annotationMethod);
                String packageName = pe == null ? "" : pe.getQualifiedName().toString();
                PackageNode pkg = new PackageNode(packageName);
                TypeNode paramType = new TypeNode(typeString, Utils.simplifyNames(typeString), pkg.getQualifiedName());

                String entryName = annotationMethod.getSimpleName().toString();
                AnnotationValue value = entry.getValue();
                Object entryValue = value.getValue();
                AnnotationElement parameter = new AnnotationElement(paramType.getQualifiedName(), entryName, entryValue.toString());
                annotationNode.addElement(parameter);
            }
        } else {
            ctx.reportWarning("Unexpected annotation element kind: " + declaredElement.getKind().toString());
        }
    }

    /// Iterates over all annotations in the API, identifying ones that are custom and
    /// those that have the `@Documented` meta-annotation and marking them as such.
    public static void markCustomAnnotations() {
        for (AppliedAnnotationNode annotation : api.getAppliedAnnotations()) {
            TypeNode localType = api.getTypeNode(annotation.getTypeName());
            if (localType != null) {
                annotation.setCustom(true);
                if (localType.hasDocumentedAnnotation()) {
                    annotation.setDocumented(true);
                }
            }
        }
    }

    /// Adds list of thrown types (exceptions) to a MethodNode based on Java model type mirrors.
    /// @param methodNode The MethodNode to add thrown types to.
    /// @param thrownTypes The list of TypeMirror representing thrown exceptions.
    public static void setThrownTypes(MethodNode methodNode, List<? extends TypeMirror> thrownTypes) {
        for (TypeMirror typeMirror : thrownTypes) {
            Element element = environment.getTypeUtils().asElement(typeMirror);
            if (element instanceof TypeElement typeElement) {
                String name = typeElement.getQualifiedName().toString();
                Reference reference = Reference.to(name)
                        .from(ctx.getPackageName())
                        .withKind(Reference.Kind.TYPE)
                        .withLabel(name);
                methodNode.addThrownType(reference);
            }
        }
    }

    /// Sets the deprecation status of a Node based on element annotations and Javadoc `@Deprecated` tag.
    /// @param node The Node to update.
    /// @param e The language model element corresponding to the node.
    /// @param dct The DocCommentTree containing javadoc comments.
    @SuppressWarnings({"squid:S1123", "squid:S1133"}) // Sonar thinks this is deprecated but it's not
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
            } else {
                node.setDeprecation(Deprecation.DEPRECATED);
            }
        }
    }

    /// Returns true if the element should be included in the public API documentation based on its modifiers and configuration.
    /// @param e The language model element to test.
    /// @return true if element is public or protected or private member documentation is configured; false otherwise.
    public static boolean isIncludedInApi(Element e) {
        Set<Modifier> mods = e.getModifiers();
        return Configuration.getDocumentPrivateMembers() || mods.contains(PUBLIC) || mods.contains(PROTECTED);
    }

    /// Sets the parameters on a MethodNode by inspecting the ExecutableElement and its doc comment tags.
    /// @param methodNode The MethodNode to update.
    /// @param ee The ExecutableElement representing the method or constructor.
    public static void setMethodParams(MethodNode methodNode, ExecutableElement ee) {
        methodNode.getParams().clear();
        DocCommentTree dct = environment.getDocTrees().getDocCommentTree(ee);
        for (VariableElement parameter : ee.getParameters()) {
            String simpleName = parameter.getSimpleName().toString();

            TypeNode paramType = getParamType(ee, simpleName);
            String paramTypeName = paramType.getQualifiedName() + paramType.getArrayBrackets();
            ParamNode param = new ParamNode(paramTypeName, simpleName);
            
            ParamTree paramTree = getParamTree(dct, parameter);
            if (paramTree != null) {
                param.setBody(createText(paramTree.getDescription()));
            }
            methodNode.addParam(param);
        }
    }

    /// Adds implementation type names to a DirectiveNode.
    /// @param directiveNode The DirectiveNode to update.
    /// @param implementations List of TypeElements representing implementations.
    public static void setImplementations(DirectiveNode directiveNode, List<? extends TypeElement> implementations) {
        for (TypeElement e : implementations) {
            String implName = e.getQualifiedName().toString();
            Reference reference = Reference.to(implName)
                    .withKind(Reference.Kind.TYPE)
                    .withLabel(implName);
            directiveNode.addImplementation(reference);
        }
    }

    /// Gets the field type as a TypeNode for the specified class name and field name.
    /// @param className Fully qualified class name containing the field.
    /// @param fieldName The field name.
    /// @return A TypeNode representing the field's type, or null if not found.
    public static TypeNode getFieldType(String className, String fieldName) {
        String qualifiedTypeName = null;
        String arrayBrackets = "";
        TypeElement classElement = environment.getElementUtils().getTypeElement(className);
        for (VariableElement field : ElementFilter.fieldsIn(classElement.getEnclosedElements())) {
            if (field.getSimpleName().toString().equals(fieldName)) {
                TypeMirror fieldType = field.asType();
                TypeMirror typeMirror = environment.getTypeUtils().getArrayType(fieldType);
                if (typeMirror != null && typeMirror.getKind() == TypeKind.ARRAY) {
                    ArrayType at = environment.getTypeUtils().getArrayType(fieldType);
                    TypeMirror componentType = at.getComponentType();
                    qualifiedTypeName = componentType.toString();
                    arrayBrackets = "\\[]";                    
                } else {
                    qualifiedTypeName = fieldType.toString();
                }
                break;
            }
        }
        if (qualifiedTypeName == null) return null;

        String simpleTypeName = Utils.simplifyNames(qualifiedTypeName);
        String packageName = getPackageName(qualifiedTypeName);
        PackageNode packageNode = api.getPackageNode(packageName);
        packageName = packageNode == null ? "" : packageNode.getQualifiedName();
        TypeNode type = new TypeNode(qualifiedTypeName, simpleTypeName, packageName);
        type.setArrayBrackets(arrayBrackets);

        return type;
    }

    /// Gets the parameter type as a TypeNode for the specified parameter name in the method.
    /// @param method The ExecutableElement representing the method.
    /// @param fieldName The parameter name.
    /// @return A TypeNode for the parameter's type, or null if not found.
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
        packageName = packageNode == null ? "" : packageNode.getQualifiedName();
        TypeNode type = new TypeNode(qualifiedTypeName, simpleTypeName, packageName);
        type.setArrayBrackets(arrayBrackets);

        return type;
    }

    /// Extracts the package name from a qualified type name.
    /// @param qualifiedTypeName The fully qualified type name.
    /// @return The package name portion or null if input null.
    private static String getPackageName(String qualifiedTypeName) {
        if (qualifiedTypeName == null) return null;
        String packageName = qualifiedTypeName;
        if (packageName.contains(".")) {
            packageName = packageName.substring(0, packageName.lastIndexOf("."));
        }
        return packageName;
    }

    /// Recursively finds the enclosing PackageElement of a given element.
    /// @param element The language model element.
    /// @return The nearest enclosing PackageElement or null if none found.
    public static PackageElement getEnclosingPackageElement(Element element) {
        Element enclosing = element.getEnclosingElement();
        if (enclosing == null) return null;
        if (enclosing.getKind() == ElementKind.PACKAGE ) {
            return (PackageElement)enclosing;
        } else {
            return getEnclosingPackageElement(enclosing);
        }
    }

    /// Recursively finds the enclosing TypeElement (class, interface, enum, record, annotation) for the given element.
    /// @param element The language model element such as a field or method.
    /// @return The enclosing TypeElement or null if none found.
    public static TypeElement getEnclosingTypeElement(Element element) {
        Element enclosing = element.getEnclosingElement();
        if (enclosing == null) {
            return null;
        }
        if (enclosing.getKind() == ElementKind.CLASS ||
                    enclosing.getKind() == ElementKind.INTERFACE ||
                    enclosing.getKind() == ElementKind.RECORD ||
                    enclosing.getKind() == ElementKind.ENUM ||
                    enclosing.getKind() == ElementKind.ANNOTATION_TYPE) {
            return (TypeElement)enclosing;
        } else {
            return getEnclosingTypeElement(enclosing);
        }
    }

    public static void setPackageSourcePath(PackageNode pkg, PackageElement ee) {
        File pkgInfo = getPackageInfoFile(ee);
        if (pkgInfo != null) {
            pkg.setHasPackageInfo(true);
            pkg.setSourcePath(pkgInfo.toPath().getParent().toString());
        }
    }

    /// Retrieves the `package-info.java` file associated with the specified 
    /// [PackageElement]. This method checks if the package element has an 
    /// associated file and returns it as a [File] object.
    ///
    /// @param packageElement the [PackageElement] for which to retrieve the 
    ///                       associated `package-info.java` file
    /// @return a [File] object representing the `package-info.java`
    ///         file if it exists; `null` if the file does not exist or is 
    ///         not associated with the given package element
    public static File getPackageInfoFile(PackageElement packageElement) {
        JavaFileObject jfo = environment.getElementUtils().getFileObjectOf(packageElement);

        if (jfo != null && jfo.getName().endsWith("package-info.java")) {
            return new File(jfo.toUri());
        }
        return null;
    }    

    public static void addJavadocToRecords(Api api) {
        for (TypeView recordView : api.getRecords()) {
            RecordTypeNode recordNode = (RecordTypeNode) recordView;
            for (MethodNode method : recordNode.getMethods()) {
                Text text = method.getFirstSentence();
                if (text.isEmpty()) {
                    switch(method.getSimpleName()) {
                        case "equals":
                            text = Text.of("Indicates whether some other object is \"equal to\" this one.");
                            break;
                        case "hashCode":
                            text = Text.of("Returns a hash code value for this object.");
                            break;
                        case "toString":
                            text = Text.of("Returns a string representation of this record class.");
                            break;
                        default:
                            text = Text.of(String.format("Returns the value of the `%s` record component.", method.getSimpleName()));
                            break;
                    }
                    method.setFirstSentence(text);
                }
            }
        }
    }
}