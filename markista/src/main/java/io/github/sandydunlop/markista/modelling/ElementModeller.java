package io.github.sandydunlop.markista.modelling;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.ModuleElement.Directive;
import javax.lang.model.element.ModuleElement.DirectiveKind;
import javax.lang.model.element.ModuleElement.ExportsDirective;
import javax.lang.model.element.ModuleElement.OpensDirective;
import javax.lang.model.element.ModuleElement.ProvidesDirective;
import javax.lang.model.element.ModuleElement.RequiresDirective;
import javax.lang.model.element.ModuleElement.UsesDirective;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;

import jdk.javadoc.doclet.DocletEnvironment;

import com.sun.source.doctree.DeprecatedTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.StartElementTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.doctree.SinceTree;

import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.AbstractMember;
import io.github.sandydunlop.markista.model.AnnotationElement;
import io.github.sandydunlop.markista.model.AnnotationNode;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.AppliedAnnotationNode;
import io.github.sandydunlop.markista.model.ClassNode;
import io.github.sandydunlop.markista.model.Deprecation;
import io.github.sandydunlop.markista.model.DirectiveNode;
import io.github.sandydunlop.markista.model.EnumNode;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.InterfaceNode;
import io.github.sandydunlop.markista.model.Link;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.Node;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.RecordNode;
import io.github.sandydunlop.markista.model.Text;
import io.github.sandydunlop.markista.model.Text.Segment;
import io.github.sandydunlop.markista.modelling.MarkdownParser.TokenKind;
import io.github.sandydunlop.markista.model.TypeNode;
import io.github.sandydunlop.markista.model.TypeReference;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ElementModeller implements Modeller<ModuleElement, PackageElement, TypeElement, VariableElement, ExecutableElement, VariableElement> {
    private Api api;
    private DocletEnvironment environment;
    private String fromPackage = "";
    private String fromType = "";
    ExecutableElement currentMethodElement;
    MethodNode currentMethodNode;

    public ElementModeller(Api a, DocletEnvironment e) {
        api = a;
        environment = e;
    }

    @Override
    public ModuleNode modelModule(ModuleElement m) {
        fromPackage = "";
        ModuleNode mod = new ModuleNode(m.getQualifiedName().toString());
        File moduleInfo = getModuleInfoFile(m);
        if (moduleInfo != null) {
            mod.setHasModuleInfo(true);
            mod.setSourcePath(moduleInfo.toPath().getParent().toString());
        }
        setDocumentation(mod, m);
        List<? extends Directive>  directives = m.getDirectives();
        for (Directive directive : directives) {
            DirectiveNode moduleDirective = createDirectiveNode(directive);
            mod.addDirective(moduleDirective);
        }
        return mod;
    }

    @Override
    public PackageNode modelPackage(PackageElement p) {
        fromType = "";
        fromPackage = p.getQualifiedName().toString();
        PackageNode pkg = new PackageNode(fromPackage);
        setPackageSourcePath(pkg, p);
        setDocumentation(pkg, p);
        return pkg;
    }

    /// Creates or retrieves a TypeNode from the supplied TypeElement.
    /// This method extracts type details such as qualified name, package, kind, ownership, modifiers, supertypes, and interfaces,
    /// adds the new TypeNode to the API model, and returns it.
    /// @param element The language model TypeElement to create a TypeNode from.
    /// @return The corresponding TypeNode in the API model, or null if unsupported or error occurs.
    @Override
    public TypeNode modelType(TypeElement element) {
        String qualifiedName = element.getQualifiedName().toString();
        PackageElement packageElement = getEnclosingPackageElement(element);
        String simpleName = element.getSimpleName().toString();
        PackageNode packageNode = api.getPackageNode(packageElement.getQualifiedName().toString());
        TypeNode typeNode = createTypeNode(simpleName, packageNode, element.getKind());
        typeNode.setQualifiedName(qualifiedName);
        fromType = qualifiedName;
        if (typeNode instanceof EnumNode enumNode) {
            setEnumConstants(enumNode, element);
        }
        setTypeOwnership(typeNode, element);
        setModifiers(typeNode, element.getModifiers());
        setAppliedAnnotations(typeNode, element);
        collectAllSupertypes(element.asType(), typeNode.getSupertypes());
        typeNode.getSupertypes().addFirst(TypeReference.to("java.lang.Object"));
        findImplementedInterfaces(element, typeNode.getImplementedInterfaces());
        setDocumentation(typeNode, element);
        setSourcePath(typeNode, element);
        return typeNode;
    }

    @Override
    public ClassNode modelClass(TypeElement type) {
        return null;
    }

    /// Creates a FieldNode representation from the VariableElement element representing a field.
    /// Links the field to the owning TypeNode.
    /// @param element The VariableElement to convert.
    /// @return The FieldNode, or null if errors occur.
    @Override
    public FieldNode modelField(VariableElement element) {
        String simpleName = element.getSimpleName().toString();
        FieldNode fieldNode = new FieldNode(element.asType().toString(), simpleName);
        fieldNode.setConstantValue((Serializable) element.getConstantValue());
        DocCommentTree dct = environment.getDocTrees().getDocCommentTree(element);
        setDocumentation(fieldNode, element);
        setModifiers(fieldNode, element.getModifiers());
        setDeprecationStatus(fieldNode, element, dct);
        setAppliedAnnotations(fieldNode, element);
        return fieldNode;
    }

    /// Creates a MethodNode representation from the ExecutableElement element (method or constructor).
    /// Sets return type, parameters, modifiers, thrown exceptions, ownership, and annotations.
    /// Adds the method to the owning TypeNode's method or constructor list.
    /// @param element The ExecutableElement to convert.
    /// @return The constructed MethodNode, or null if errors occur.
    @Override
    public MethodNode modelMethod(ExecutableElement element) {
        String qualifiedTypeName = element.getReturnType().toString();
        fromType = qualifiedTypeName;
        MethodNode methodNode = new MethodNode(qualifiedTypeName, element.getSimpleName().toString());
        // setMethodParams must be called before setMethodOwnerDetails as the method
        // parameters need to be present to determine if this method already exists.
        setMethodParams(methodNode, element);
        if (!setMethodOwnerDetails(methodNode, element)) {
            return null;
        }

        currentMethodElement = element; // Used for @inheritDoc
        currentMethodNode = methodNode;

        setModifiers(methodNode, element.getModifiers());
        setThrownTypes(methodNode, element.getThrownTypes());
        setMethodAnnotations(methodNode, element);
        setAppliedAnnotations(methodNode, element);
        DocCommentTree dct = environment.getDocTrees().getDocCommentTree(element);
        setDeprecationStatus(methodNode, element, dct);
        if (dct != null) {
            methodNode.setFirstSentence(createText(dct.getFirstSentence()));
            methodNode.setBody(createText(dct.getBody()));
            methodNode.setFullBody(createText(dct.getFullBody()));
            ReturnTree returnTree = getReturnTree(dct);
            if (returnTree != null) {
                methodNode.setReturnDescription(createText(returnTree.getDescription()));
            }
            methodNode.setReferences(getReferences(dct));
            methodNode.setSince(getSince(dct));
        }
        return methodNode;
    }

    @Override
    public ParamNode modelParam(VariableElement element) {
        return null;
    }

    /// Factory method to create TypeNode (ClassNode, InterfaceNode, RecordNode,
    /// EnumNode, or AnnotationNode) based on ElementKind.
    /// @param simpleName The simple (unqualified) name of the type.
    /// @param packageNode The owning PackageNode.
    /// @param elementKind The ElementKind representing the type kind.
    /// @return A TypeNode instance corresponding to the kind, or null if unsupported.
    private TypeNode createTypeNode(String simpleName, PackageNode packageNode, ElementKind elementKind) {
        return switch (elementKind) {
            case ElementKind.CLASS -> new ClassNode(simpleName, packageNode.getName());
            case ElementKind.INTERFACE -> new InterfaceNode(simpleName, packageNode.getName());
            case ElementKind.RECORD -> new RecordNode(simpleName, packageNode.getName());
            case ElementKind.ENUM -> new EnumNode(simpleName, packageNode.getName());
            case ElementKind.ANNOTATION_TYPE -> new AnnotationNode(simpleName, packageNode.getName());
            default -> null;
        };
    }

    /// Reads enum constants from the TypeElement and adds them as FieldNodes to the EnumNode.
    /// @param enumNode The EnumNode to populate with constants.
    /// @param e The TypeElement representing the enum type.
    public void setEnumConstants(EnumNode enumNode, TypeElement e) {
        List<? extends Element> enclosedElements = e.getEnclosedElements();
        for (Element element : enclosedElements) {
            if (element.getKind() == ElementKind.ENUM_CONSTANT) {
                String typeName = "var";
                TypeMirror tm = element.asType();
                if (tm != null) {
                    typeName = tm.toString();
                }
                FieldNode enumConstant = new FieldNode(typeName, element.getSimpleName().toString());
                enumNode.getConstants().add(enumConstant);
            }
        }
    }

    /// Sets ownership of a TypeNode based on its enclosing type or package.
    /// Updates the ownership link and qualified names accordingly.
    /// @param typeNode The TypeNode to set ownership on.
    /// @param element The TypeElement representing the type.
    public void setTypeOwnership(TypeNode typeNode, TypeElement element) {
        if (typeNode == null) return;
        TypeElement owner = getEnclosingTypeElement(element);
        TypeNode ownerTypeNode = owner == null ? null : api.getTypeNode(owner.getQualifiedName().toString());
        if (ownerTypeNode != null) {
            // Owner is a type (class, interface, enum, annotation)
            typeNode.setOwnerName(ownerTypeNode.getQualifiedName());
            typeNode.setSimpleName(ownerTypeNode.getSimpleName() + "." + typeNode.getSimpleName());
            ownerTypeNode.addType(typeNode);
        } else {
            // Owner is a package
            PackageNode ownerPackage = api.getPackageNode(typeNode.getPackageName());
            typeNode.setOwnerName(ownerPackage.getName());
            ownerPackage.addType(typeNode);
        }
    }

    /// Adds modifiers to a model Node based on the set of language model modifiers.
    /// @param node The Node to add modifiers to.
    /// @param modifiers The set of Modifier enums from language model.
    public void setModifiers(AbstractMember node, Set<Modifier> modifiers) {
        for (Modifier modifier : modifiers) {
            io.github.sandydunlop.markista.model.Modifier mod =
                io.github.sandydunlop.markista.model.Modifier.valueOf(modifier.name());
            node.addModifier(mod);
        }
    }


    /// Adds applied annotations to a TypeNode, FieldNode, or MethodNode.
    /// @param node The TypeNode to add annotations to.
    /// @param elem The scanned Element that contains information about the applied annotations.
    public void setAppliedAnnotations(AbstractMember node, Element elem) {
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
    public void setAppliedAnnotation(AbstractMember node, AnnotationMirror annotationMirror) {
        DeclaredType declaredType = annotationMirror.getAnnotationType();
        Element declaredElement = declaredType.asElement();
        if (declaredElement instanceof TypeElement declaredTypeElement) {
            AppliedAnnotationNode appliedAnnotation = new AppliedAnnotationNode(
                    declaredTypeElement.getQualifiedName().toString());
            node.addAppliedAnnotation(appliedAnnotation);
            api.addAppliedAnnotation(appliedAnnotation);
            if (declaredTypeElement.getQualifiedName().toString().equals("java.lang.annotation.Documented") &&
                    node instanceof AnnotationNode annotationNode) {
                annotationNode.setHasDocumentedAnnotation(true);
            }
            Map<? extends ExecutableElement, ? extends AnnotationValue> values = annotationMirror.getElementValues();
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values.entrySet()) {
                ExecutableElement annotationMethod = entry.getKey();
                TypeMirror methodType = annotationMethod.asType();

                String typeString = methodType.toString();
                if (typeString != null && typeString.charAt(0) == '(') {
                    typeString = typeString.substring(2);
                }

                PackageElement pe = getEnclosingPackageElement(annotationMethod);
                String packageName = pe == null ? "" : pe.getQualifiedName().toString();
                PackageNode pkg = new PackageNode(packageName);
                TypeNode paramType = new TypeNode(Context.NameSimplifier.simplifyNames(typeString), pkg.getName());

                String entryName = annotationMethod.getSimpleName().toString();
                AnnotationValue value = entry.getValue();
                Object entryValue = value.getValue();
                AnnotationElement parameter = new AnnotationElement(paramType.getQualifiedName(), entryName, entryValue.toString());
                appliedAnnotation.addElement(parameter);
            }
        }
    }

    public void setSourcePath(TypeNode typeNode, TypeElement element) {
        JavaFileObject jfo = environment.getElementUtils().getFileObjectOf(element);
        if (jfo != null) {
            typeNode.setSourcePath(Path.of(jfo.toUri()).toString());
            PackageNode typePackage = api.getPackageNode(typeNode.getPackageName());
            if (typePackage != null && typePackage.getSourcePath() == null) {
                typePackage.setSourcePath(Path.of(jfo.toUri()).getParent().toString());
            }
        }
    }

    /// Sets the parameters on a MethodNode by inspecting the ExecutableElement and its doc comment tags.
    /// @param methodNode The MethodNode to update.
    /// @param ee The ExecutableElement representing the method or constructor.
    public void setMethodParams(MethodNode methodNode, ExecutableElement ee) {
        methodNode.getParams().clear();
        DocCommentTree dct = environment.getDocTrees().getDocCommentTree(ee);
        for (VariableElement parameter : ee.getParameters()) {
            String simpleName = parameter.getSimpleName().toString();
            String paramTypeName = getParamType(ee, simpleName);
            ParamNode param = new ParamNode(paramTypeName, simpleName);
            ParamTree paramTree = getParamTree(dct, parameter);
            if (paramTree != null) {
                param.setBody(createText(paramTree.getDescription()));
            }
            methodNode.addParam(param);
        }
    }

    public boolean setMethodOwnerDetails(MethodNode methodNode, ExecutableElement element) {
        TypeElement ownerElement = getEnclosingTypeElement(element);
        if (ownerElement != null) {
            methodNode.setOwnerName(ownerElement.getQualifiedName().toString());
            api.addMethod(methodNode);
            if (element.getKind() == ElementKind.METHOD) {
                methodNode.setConstructor(false);
            } else if (element.getKind() == ElementKind.CONSTRUCTOR) {
                methodNode.setConstructor(true);
            }
            return true;
        } else {
            return false;
        }
    }

    /// Sets the deprecation status of a Node based on element annotations and Javadoc `@Deprecated` tag.
    /// @param node The Node to update.
    /// @param e The language model element corresponding to the node.
    /// @param dct The DocCommentTree containing javadoc comments.
    @SuppressWarnings({"squid:S1123", "squid:S1133"}) // Sonar thinks this is deprecated but it's not
    public void setDeprecationStatus(Node node, Element e, DocCommentTree dct) {
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

    /// Adds list of thrown types (exceptions) to a MethodNode based on Java model type mirrors.
    /// @param methodNode The MethodNode to add thrown types to.
    /// @param thrownTypes The list of TypeMirror representing thrown exceptions.
    public void setThrownTypes(MethodNode methodNode, List<? extends TypeMirror> thrownTypes) {
        for (TypeMirror typeMirror : thrownTypes) {
            Element element = environment.getTypeUtils().asElement(typeMirror);
            if (element instanceof TypeElement typeElement) {
                String name = typeElement.getQualifiedName().toString();
                Link reference = Link.to(name)
                        .fromPackage(fromPackage)
                        .withKind(Link.Kind.TYPE)
                        .withLabel(name);
                methodNode.addThrownType(reference);
            }
        }
    }

    /// Sets annotations on the MethodNode, in particular looks for @Override annotation to set overridden methods.
    /// @param method The MethodNode to update.
    /// @param methodElement The ExecutableElement representing the method.
    public void setMethodAnnotations(MethodNode method, ExecutableElement methodElement) {
        for (AnnotationMirror anno : methodElement.getAnnotationMirrors()) {
            DeclaredType declaredType = anno.getAnnotationType();
            Element typeElement = declaredType.asElement();
            if ("Override".equals(typeElement.getSimpleName().toString())) {
                Link link = new Link().withKind(Link.Kind.METHOD).withMethodName(method.getSimpleName());
                method.setBaseMethod(link);
            }
        }
    }






    /// Finds the @return tag from a Javadoc DocCommentTree if present.
    /// @param dcTree The DocCommentTree to search.
    /// @return The ReturnTree if found, null otherwise.
    public ReturnTree getReturnTree(DocCommentTree dcTree) {
        if (dcTree == null) return null;
        for (DocTree docTree : dcTree.getBlockTags()) {
            if (docTree instanceof ReturnTree tree) {
                return tree;
            }
        }
        return null;
    }

    /// Extracts a list of Reference objects representing occurrences of @see tags in the Javadoc comment.
    /// @param dcTree The DocCommentTree to process.
    /// @return A list of Reference objects extracted from @see tags.
    public List<Link> getReferences(DocCommentTree dcTree) {
        if (dcTree == null) return new ArrayList<>();
        List<Link> refs = new ArrayList<>();
        for (DocTree tagTree : dcTree.getBlockTags()) {
            if (tagTree instanceof SeeTree seeTree) {
                List<? extends DocTree> see = seeTree.getReference();
                for (DocTree docRef : see) {
                    if (docRef.getKind() == DocTree.Kind.MARKDOWN) {
                        // This is sometimes (always?!) HTML, not Markdown?
                        refs.add(Link.to(getUrl(docRef.toString())));
                    } else if (docRef.getKind() == DocTree.Kind.REFERENCE) {
                        refs.add(Link.to(docRef.toString()).withKind(Link.Kind.TYPE));
                    }
                }
            }
        }
        return refs;
    }

    /// Extracts a URL string from html-like text, e.g., from an href attribute inside double-quotes.
    /// @param html The input HTML-like string.
    /// @return The extracted URL inside quotes or null if none found.
    public String getUrl(String html) {
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

    /// Extracts the @since tag content from a DocCommentTree, if present.
    /// @param dcTree The DocCommentTree containing tags.
    /// @return A Text object representing @since content or an empty Text if none present.
    public Text getSince(DocCommentTree dcTree) {
        if (dcTree == null) return null;
        for (DocTree tagTree : dcTree.getBlockTags()) {
            if (tagTree instanceof SinceTree sinceTree) {
                return createText(sinceTree.getBody());
            }
        }
        return Text.empty();
    }

    /// Finds the DeprecatedTree from a Javadoc DocCommentTree if present.
    /// @param docComment The Javadoc comment tree.
    /// @return The DeprecatedTree if found, null otherwise.
    public DeprecatedTree getDeprecation(DocCommentTree docComment) {
        if (docComment == null) return null;
        for (DocTree docTree : docComment.getBlockTags()) {
            if (docTree instanceof DeprecatedTree tree) {
                return tree;
            }
        }
        return null;
    }

    /// Gets the parameter type as a TypeNode for the specified parameter name in the method.
    /// @param method The ExecutableElement representing the method.
    /// @param fieldName The parameter name.
    /// @return A TypeNode for the parameter's type, or null if not found.
    public String getParamType(ExecutableElement method, String fieldName) {
        for (VariableElement param : method.getParameters()) {
            if (param.getSimpleName().toString().equals(fieldName)){
                return param.asType().toString();
            }
        }
        return "";
    }

    /// Finds the @param tag in a DocCommentTree matching the specified parameter variable.
    /// @param dcTree The DocCommentTree containing block tags.
    /// @param parameter The VariableElement parameter to match.
    /// @return The matching ParamTree if found, null otherwise.
    public ParamTree getParamTree(DocCommentTree dcTree, VariableElement parameter) {
        if (dcTree == null) return null;
        for (DocTree tagTree : dcTree.getBlockTags()) {
            if (tagTree instanceof ParamTree tree && tree.getName().getName().toString().equals(parameter.getSimpleName().toString())) {
                return tree;
            }
        }
        return null;
    }

    /// Recursively finds the enclosing PackageElement of a given element.
    /// @param element The language model element.
    /// @return The nearest enclosing PackageElement or null if none found.
    private PackageElement getEnclosingPackageElement(Element element) {
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
    public TypeElement getEnclosingTypeElement(Element element) {
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

    /// Retrieve the module-info.java file for the given ModuleElement if available.
    /// This helper inspects the JavaFileObject associated with the module and returns
    /// a File when the file name ends with "module-info.java".
    /// @param moduleElement the ModuleElement to inspect
    /// @return a File pointing to the module-info.java source or null if none found
    private File getModuleInfoFile(ModuleElement moduleElement) {
        JavaFileObject jfo = environment.getElementUtils().getFileObjectOf(moduleElement);

        if (jfo != null && jfo.getName().endsWith("module-info.java")) {
            return new File(jfo.toUri());
        }
        return null;
    }

    private void setPackageSourcePath(PackageNode pkg, PackageElement ee) {
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
    private File getPackageInfoFile(PackageElement packageElement) {
        JavaFileObject jfo = environment.getElementUtils().getFileObjectOf(packageElement);

        if (jfo != null && jfo.getName().endsWith("package-info.java")) {
            return new File(jfo.toUri());
        }
        return null;
    }

    /// Sets documentation text for a Node based on the doc comment tree attached to a language model element.
    /// This populates first sentence, body, and full body texts.
    /// @param node The Node to set documentation for.
    /// @param e The element whose doc comment is used.
    private void setDocumentation(Node node, Element e) {
        DocCommentTree dct = environment.getDocTrees().getDocCommentTree(e);
        if (dct != null) {
            node.setFirstSentence(createText(dct.getFirstSentence()));
            node.setBody(createText(dct.getBody()));
            node.setFullBody(createText(dct.getFullBody()));
        }
    }

    /// Creates a complete Text object by traversing a list of DocTree nodes from the Javadoc comment.
    /// @param dtList List of DocTree nodes representing a part of a Javadoc comment.
    /// @return A Text object composed of segments derived from each DocTree node.
    private Text createText(List<? extends DocTree> dtList) {
        Text text = Text.empty();
        for (DocTree docTree : dtList) {
            text.append(docTreeToText(docTree));
        }
        return text;
    }

    /// Creates a [Text] object from a DocTree node, setting the appropriate kind and content.
    /// @param docTree The DocTree node to convert.
    /// @return A [Text] object representing the content and kind of the provided DocTree.
    Text docTreeToText(DocTree docTree) {
        Text text = Text.empty();
        Text.Segment segment = Text.Segment.empty();
        switch(docTree.getKind()) {
            case MARKDOWN:
                text = markdownToText(docTree.toString());
                break;
            case TEXT:
                text.append(docTree.toString());
                break;
            case LINK:
                segment.setKind(Segment.Kind.LINK);
                Link link = Link.to(getDocTreePart(docTree, 1))
                        .fromPackage(fromPackage)
                        .fromType(fromType);
                api.addLink(link);
                segment.setLink(link);
                text.append(segment);
                break;
            case LINK_PLAIN:
                segment.setKind(Segment.Kind.LINK);
                link = Link.to(getDocTreePart(docTree, 1))
                        .fromPackage(fromPackage)
                        .fromType(fromType);
                segment.setLink(link);
                text.append(segment);
                api.addLink(link);
                if (docTree instanceof LinkTree linkTree) {
                    segment.setText(createText(linkTree.getLabel()).toString());
                    link.setLabel(createText(linkTree.getLabel()).toString());
                }
                break;
            case CODE:
                segment.setKind(Segment.Kind.CODE);
                segment.setText(getDocTreeText(docTree, 1));
                text.append(segment);
                break;
            case START_ELEMENT:
                segment.setKind(Segment.Kind.TEXT);
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
                segment.setKind(Segment.Kind.INHERIT);
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
    Text markdownToText(String markdown) {
        Text text = Text.empty();
        MarkdownParser parser = new MarkdownParser(markdown);
        MarkdownParser.Token token = parser.firstToken();
        while (token.getKind() != MarkdownParser.TokenKind.END) {
            if (token.getKind() == MarkdownParser.TokenKind.BRACKETS_TAG) {
                MarkdownParser.Token next = token.getNext();
                token = handleBracketsTag(token, next, text);
            } else if (token.getKind() == TokenKind.TEXT) {
                text.append(token.getText());
            }
            token = token.getNext();
        }
        return text;
    }

    private MarkdownParser.Token handleBracketsTag(MarkdownParser.Token token, MarkdownParser.Token next, Text text) {
        if (next.getKind() == TokenKind.BRACKETS_TAG || next.getKind() == TokenKind.PARENS_TAG) {
            Link ref = Link.to(next.getText())
                    .fromPackage(fromPackage)
                    .fromType(fromType);
            ref.setLabel(token.getText());
            Segment segment = Segment.empty()
                    .setKind(Segment.Kind.LINK)
                    .setLink(ref)
                    .setText(token.getText());
            text.append(segment);
            api.addLink(ref);
            return next;
        } else {
            if (token.getText().contains(" ")) {
                Segment segment = Segment.empty()
                        .setKind(Segment.Kind.TEXT)
                        .setText(token.getText());
                text.append(segment);
            } else {
                Link ref = Link.to(token.getText())
                        .fromPackage(fromPackage)
                        .fromType(fromType);
                Segment segment = Segment.empty()
                        .setKind(Segment.Kind.LINK)
                        .setLink(ref);
                text.append(segment);
                api.addLink(ref);
            }
            return token;
        }
    }

    /// Extracts text from a DocTree to build a string from a part of its tokenized representation.
    /// @param docTree The DocTree to extract from.
    /// @param start The starting index for extraction.
    /// @return A string representing the extracted part or empty string if extraction fails.
    private String getDocTreeText(DocTree docTree, int start) {
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
    private String getDocTreePart(DocTree docTree, int n) {
        String input = docTree.toString();
        String[] parts = input.split(" ");
        if (n < parts.length) {
            parts[parts.length-1] = parts[parts.length-1].substring(0, parts[parts.length-1].length() - 1);
            return parts[n];
        }
        return "";
    }

    /// Collects all supertypes (classes) of the specified type recursively and adds them to the result list.
    /// java.lang.Object is excluded.
    /// @param t The type to examine.
    /// @param result The list to receive supertypes.
    public void collectAllSupertypes(TypeMirror t, List<TypeReference> result) {
        for (TypeMirror s : environment.getTypeUtils().directSupertypes(t)) {
            if (result != null) {
                String name = s.toString();
                if (!"java.lang.Object".equals(name)) {
                    if (!isInterface(s)){
                        result.addFirst(TypeReference.to(name));
                    }
                    collectAllSupertypes(s, result);
                }
            }
        }
    }

    /// Returns true if the TypeMirror represents an interface.
    /// @param typeMirror The TypeMirror to check.
    /// @return true if the type is an interface, false otherwise.
    public boolean isInterface(TypeMirror typeMirror) {
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
    public void findImplementedInterfaces(TypeElement typeElement, List<TypeReference> result) {
        List<? extends TypeMirror> interfaces = typeElement.getInterfaces();
        for (TypeMirror interfaceType : interfaces) {
            result.add(TypeReference.to(interfaceType.toString()));
        }
    }

    /// Creates a [DirectiveNode] to encapsulate the information provided
    /// by a module directive element.
    /// @param directive a scanned module directive.
    /// @return A DirectiveNode representing the scanned directive element.
    public DirectiveNode createDirectiveNode(Directive directive) {
        return switch (directive.getKind()) {
            case DirectiveKind.REQUIRES -> createRequiresDirective(directive);
            case DirectiveKind.EXPORTS -> createExportsDirective(directive);
            case DirectiveKind.OPENS -> createOpensDirective(directive);
            case DirectiveKind.USES -> createUsesDirective(directive);
            case DirectiveKind.PROVIDES -> createProvidesDirective(directive);
        };
    }

    /// Creates a DirectiveNode representing a [requires](javax.lang.model.element.ModuleElement.RequiresDirective) directive.
    /// @param directive a scanned [RequiresDirective](javax.lang.model.element.ModuleElement.RequiresDirective) element.
    /// @return A DirectiveNode representing the scanned directive element.
    public DirectiveNode createRequiresDirective(Directive directive) {
        DirectiveNode.Kind kind = DirectiveNode.Kind.REQUIRES;
        RequiresDirective requires = (RequiresDirective) directive;
        Element dependency = requires.getDependency();
        ModuleElement directiveModuleElement = environment.getElementUtils().getModuleOf(dependency);
        String name = directiveModuleElement.getQualifiedName().toString();
        boolean transitive = requires.isTransitive();
        Link reference = Link.to(name)
                .withKind(Link.Kind.MODULE)
                .withLabel(name);
        return new DirectiveNode(kind, reference, transitive);
    }

    /// Creates a DirectiveNode representing an [exports](javax.lang.model.element.ExportsDirective) directive.
    /// @param directive a scanned [ExportsDirective](javax.lang.model.element.ExportsDirective) element.
    /// @return A DirectiveNode representing the scanned directive element.
    public DirectiveNode createExportsDirective(Directive directive) {
        DirectiveNode.Kind kind = DirectiveNode.Kind.EXPORTS;
        ExportsDirective exports = (ExportsDirective) directive;
        PackageElement directivePackageElement = exports.getPackage();
        String name = directivePackageElement.getQualifiedName().toString();
        List<? extends ModuleElement> modules = exports.getTargetModules();
        Link ref = Link.to(name)
                .withKind(Link.Kind.PACKAGE)
                .withLabel(name);
        DirectiveNode directiveNode = new DirectiveNode(kind, ref);
        if (modules != null) {
            for (ModuleElement moduleElement : modules) {
                String packageName = moduleElement.getQualifiedName().toString();
                Link reference = Link.to(packageName)
                        .withKind(Link.Kind.PACKAGE)
                        .withLabel(packageName);
                directiveNode.addPackage(reference);
            }
        }
        return directiveNode;
    }

    /// Creates a DirectiveNode representing an [opens](javax.lang.model.element.OpensDirective) directive.
    /// @param directive a scanned [OpensDirective](javax.lang.model.element.OpensDirective) element.
    /// @return A DirectiveNode representing the scanned directive element.
    public DirectiveNode createOpensDirective(Directive directive) {
        DirectiveNode.Kind kind = DirectiveNode.Kind.OPENS;
        OpensDirective opens = (OpensDirective) directive;
        PackageElement directivePackageElement = opens.getPackage();
        String name = directivePackageElement.getQualifiedName().toString();
        List<? extends ModuleElement> modules = opens.getTargetModules();
        Link ref = Link.to(name)
                .withKind(Link.Kind.PACKAGE)
                .withLabel(name);
        DirectiveNode directiveNode = new DirectiveNode(kind, ref);
        if (modules != null) {
            for (ModuleElement moduleElement : modules) {
                String moduleName = moduleElement.getQualifiedName().toString();
                Link reference = Link.to(moduleName)
                        .withKind(Link.Kind.PACKAGE)
                        .withLabel(moduleName);
                directiveNode.addPackage(reference);
            }
        }
        return directiveNode;
    }

    /// Creates a DirectiveNode representing a [uses](javax.lang.model.element.UsesDirective) directive.
    /// @param directive a scanned [UsesDirective](javax.lang.model.element.UsesDirective) element.
    /// @return A DirectiveNode representing the scanned directive element.
    public DirectiveNode createUsesDirective(Directive directive) {
        DirectiveNode.Kind kind = DirectiveNode.Kind.USES;
        UsesDirective uses = (UsesDirective) directive;
        String name = uses.getService().getQualifiedName().toString();
        Link ref = Link.to(name)
                .withKind(Link.Kind.TYPE)
                .withLabel(name);
        return new DirectiveNode(kind, ref);
    }

    /// Creates a DirectiveNode representing a [provides](javax.lang.model.element.ProvidesDirective) directive.
    /// @param directive a scanned [ProvidesDirective](javax.lang.model.element.ProvidesDirective) element.
    /// @return A DirectiveNode representing the scanned directive element.
    public DirectiveNode createProvidesDirective(Directive directive) {
        DirectiveNode.Kind kind = DirectiveNode.Kind.PROVIDES;
        ProvidesDirective provides = (ProvidesDirective) directive;
        TypeElement service = provides.getService();
        String name = service.getQualifiedName().toString();
        Link ref = Link.to(name)
                .withKind(Link.Kind.TYPE)
                .withLabel(name);
        DirectiveNode directiveNode = new DirectiveNode(kind, ref);
        setImplementations(directiveNode, provides.getImplementations());
        String interfaceName = service.getQualifiedName().toString();
        Link reference = Link.to(interfaceName)
                .withKind(Link.Kind.PACKAGE)
                .withLabel(interfaceName);
        directiveNode.setInterface(reference);
        return directiveNode;
    }

    /// Adds implementation type names to a DirectiveNode.
    /// @param directiveNode The DirectiveNode to update.
    /// @param implementations List of TypeElements representing implementations.
    public void setImplementations(DirectiveNode directiveNode, List<? extends TypeElement> implementations) {
        for (TypeElement e : implementations) {
            String implName = e.getQualifiedName().toString();
            Link reference = Link.to(implName)
                    .withKind(Link.Kind.TYPE)
                    .withLabel(implName);
            directiveNode.addImplementation(reference);
        }
    }
}
