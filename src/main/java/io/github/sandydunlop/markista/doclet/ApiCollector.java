package io.github.sandydunlop.markista.doclet;

import com.sun.source.doctree.DeprecatedTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTree.Kind;
import com.sun.source.doctree.ErroneousTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.doctree.SinceTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTrees;

import io.github.sandydunlop.markista.model.AnnotationNode;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ClassNode;
import io.github.sandydunlop.markista.model.Deprecation;
import io.github.sandydunlop.markista.model.EnumNode;
import io.github.sandydunlop.markista.model.ExceptionNode;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.InterfaceNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.Node;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.Reference;
import io.github.sandydunlop.markista.model.Text;
import io.github.sandydunlop.markista.model.TypeNode;
import io.github.sandydunlop.markista.util.NameUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.ElementScanner9;
import javax.lang.model.util.Elements;

import jdk.javadoc.doclet.DocletEnvironment;

import static javax.lang.model.element.Modifier.*;

/// A class that scans code and generates an API tree representing code and Javadoc comments.
public class ApiCollector extends ElementScanner9<Void, Integer> {
    private PackageNode packageDoc = null;
    private static DocTrees treeUtils;
    private static Types typeUtils;
    private static Elements elementUtils;
    private Api api;
    private boolean documentPrivateMembers = false;
    private Set<Element> encounteredSupertypes = new HashSet<>();

    public ApiCollector(DocletEnvironment environment) {
        typeUtils = environment.getTypeUtils();
        treeUtils = environment.getDocTrees();
        elementUtils = environment.getElementUtils();
        api = new Api();
    }

    public void setDocumentPrivateMembers(boolean documentPrivateMembers) {
        this.documentPrivateMembers = documentPrivateMembers;
    }

    public Api collect(Set<? extends Element> elements) {
        scan(elements, 0);
        addConstantFieldValuesReference();
        return api;
    }

    @Override
    public Void scan(Element e, Integer depth) {
        return super.scan(e, depth + 1);
    }

    @Override
    public Void visitPackage(PackageElement ee, Integer depth) {
        PackageNode pkg = api.getPackageDoc(ee.getQualifiedName().toString());
        if (pkg == null) {
            DocCommentTree dct = treeUtils.getDocCommentTree(ee);
            pkg = new PackageNode(ee.getQualifiedName().toString());
            if (dct != null) {
                pkg.setFirstSentence(dct.getFirstSentence());
                pkg.setBody(dct.getBody());
                pkg.setFullBody(dct.getFullBody());
            }
            api.addPackage(pkg);
            packageDoc = pkg;
            Element enclosing = ee.getEnclosingElement();
            if (enclosing.getKind() == ElementKind.PACKAGE) {
                PackageElement p = (PackageElement)ee;
                PackageNode owner = api.getPackageDoc(p.getQualifiedName().toString());
                if (owner != null) {
                    owner.getPackages().add(pkg);
                }
            }
        }
        return super.visitPackage(ee, depth);
    }

    @Override
    public Void visitType(TypeElement e, Integer depth) { 
        if (isIncludedInApi(e)){
            PackageElement packageElement = getEnclosingPackageElement(e);
            String simpleName = e.getSimpleName().toString();
            String qualifiedName = e.getQualifiedName().toString();
            String packageName = packageElement.getQualifiedName().toString();
            TypeNode typeDoc = insantiateSubtype(e.getKind(), qualifiedName, simpleName, packageName);
            DocCommentTree dct = treeUtils.getDocCommentTree(e);
            if (dct != null) {
                typeDoc.setFirstSentence(dct.getFirstSentence());
                typeDoc.setBody(dct.getBody());
                typeDoc.setFullBody(dct.getFullBody());
            }
            typeDoc.modifiers.addAll(e.getModifiers());
            collectAllSupertypes(e.asType(), typeDoc.supertypes);
            typeDoc.supertypes.add(0, "java.lang.Object");
            findImplementedInterfaces(e, typeDoc.implementedInterfaces);
            TypeElement owner = getEnclosingTypeElement(e);
            ClassNode ownerClass = owner == null ? null : api.getClassDoc(owner);
            if (ownerClass != null) {
                typeDoc.owner = ownerClass;
                typeDoc.simpleName = ownerClass.simpleName + "." + typeDoc.simpleName;
            }else{
                typeDoc.owner = packageDoc;
            }
            if (typeDoc instanceof ClassNode) {
                ClassNode doc = (ClassNode)api.getTypeDoc(qualifiedName, api.getClasses());
                if (doc == null) {
                    doc = (ClassNode)typeDoc;
                    if (ownerClass != null) {
                        // Owner is a class
                        ownerClass.classes.add(doc);
                    }else{
                        // Owner is a package
                        packageDoc.classes.add(doc);
                    }
                    api.addClass(doc);
                }
            } else if (typeDoc instanceof InterfaceNode) {
                InterfaceNode doc = (InterfaceNode)api.getTypeDoc(qualifiedName, api.getInterfaces());
                if (doc == null) {
                    doc = (InterfaceNode)typeDoc;
                    if (ownerClass != null) {
                        // Owner is a class
                        ownerClass.interfaces.add(doc);
                    }else{
                        // Owner is a package
                        packageDoc.interfaces.add(doc);
                    }
                    api.addInterface(doc);
                }
            } else if (typeDoc instanceof EnumNode) {
                EnumNode doc = (EnumNode)api.getTypeDoc(qualifiedName, api.getEnums());
                if (doc == null) {
                    doc = (EnumNode)typeDoc;
                    List<? extends Element> enclosedElements = e.getEnclosedElements();
                    for (Element element : enclosedElements) {
                        if (element.getKind() == ElementKind.ENUM_CONSTANT) {
                            FieldNode enumConstant = new FieldNode(null, element.getSimpleName().toString());
                            doc.constants.add(enumConstant);
                        }
                    }
                    if (ownerClass != null) {
                        // Owner is a class
                        ownerClass.enumClasses.add(doc);
                    }else{
                        // Owner is a package
                        packageDoc.enumClasses.add(doc);
                    }
                    api.addEnum(doc);
                }
            } else if (typeDoc instanceof ExceptionNode) {
                ExceptionNode doc = (ExceptionNode)api.getTypeDoc(qualifiedName, api.getEnums());
                if (doc == null) {
                    doc = (ExceptionNode)typeDoc;
                    if (ownerClass != null) {
                        // Owner is a class
                        ownerClass.exceptionClasses.add(doc);
                    }else{
                        // Owner is a package
                        packageDoc.exceptionClasses.add(doc);
                    }
                    api.addException(doc);
                }
            } else if (typeDoc instanceof AnnotationNode) {
                AnnotationNode doc = (AnnotationNode)api.getTypeDoc(qualifiedName, api.getEnums());
                if (doc == null) {
                    doc = (AnnotationNode)typeDoc;
                    if (ownerClass != null) {
                        // Owner is a class
                        ownerClass.annotationClasses.add(doc);
                    }else{
                        // Owner is a package
                        packageDoc.annotationClasses.add(doc);
                    }
                    api.addAnnotation(doc);
                }
            }
        }
        
        return super.visitType(e, depth);
    }

    @Override
    public Void visitExecutable(ExecutableElement ee, Integer depth) {
        if (isIncludedInApi(ee)){
            TypeMirror tm = ee.getReturnType();

            // The return type...
            String qualifiedName = tm.toString();
            String simpleName = NameUtils.simplifyNames(qualifiedName);
            PackageElement packageElement = getEnclosingPackageElement(ee);
            String packageName = packageElement.getQualifiedName().toString();                
            TypeNode returnType = new TypeNode(qualifiedName, simpleName, packageName);

            MethodNode method = new MethodNode(returnType, ee.getSimpleName().toString());
            method.modifiers.addAll(ee.getModifiers()); 
            DocCommentTree dct = treeUtils.getDocCommentTree(ee);
            if (dct != null) {
                method.setFirstSentence(dct.getFirstSentence());
                method.setBody(dct.getBody());
                method.setFullBody(dct.getFullBody());
                ReturnTree returnTree = getReturnTree(dct);
                if (returnTree != null) {
                    method.setReturnComment(returnTree.getDescription());
                }
                method.setReferences(getReferences(dct));
                method.since = getSince(dct);
            }

            method.thrownTypes = ee.getThrownTypes(); //TODO convert to model.* types
            setDeprecationStatus(method, ee, dct);
            TypeElement ownerClassElement = getEnclosingTypeElement(ee);
            ClassNode ownerClass = api.getClassDoc(ownerClassElement);
            setMethodParams(method, ee);

            if (ee.getKind() == ElementKind.METHOD) {
                if (ownerClass != null) {
                    // It's owned by a class.
                    MethodNode existingMethodDoc = ownerClass.getMethod(method);
                    if (existingMethodDoc == null) {   
                        ownerClass.methods.add(method);
                    }
                }else{
                    // TODO: Could be owned by an enum or an interface.
                }
            }else if (ee.getKind() == ElementKind.CONSTRUCTOR) {
                if (ownerClass != null) {
                    // It's owned by a class.
                    method.simpleName = ownerClass.simpleName;
                    MethodNode existingMethodDoc = ownerClass.getConstructor(method);
                    if (existingMethodDoc == null) {
                        ownerClass.constructors.add(method);
                    }
                }else{
                    // TODO: Could be owned by an enum or an interface.
                }
            }else{
                //TODO: Might belong to an ENUM
            }
        }
        return super.visitExecutable(ee, depth);
    }

    @Override
    public Void visitVariable(VariableElement ve, Integer depth) {
        if (isIncludedInApi(ve)){
            if (ve.getKind() == ElementKind.FIELD) {
                TypeElement classElement = getEnclosingTypeElement(ve);
                ClassNode classDoc = api.getClassDoc(classElement);
                if (classDoc != null) {
                    String simpleName = ve.getSimpleName().toString();
                    FieldNode fieldDoc = classDoc.getField(simpleName);
                    if (fieldDoc == null) {
                        String qualifiedClassName = classElement.getQualifiedName().toString();

                        // String qualifiedTypeName = getFieldType(elementUtils, qualifiedClassName, simpleName);
                        // String simpleTypeName = NameUtils.simplifyNames(qualifiedTypeName);
                        // String packageName = getPackageName(qualifiedTypeName);
                        // TypeNode type = new TypeNode(qualifiedTypeName, simpleTypeName, packageName);

                        TypeNode type = getFieldType(elementUtils, qualifiedClassName, simpleName);
 
                        fieldDoc = new FieldNode(type, simpleName);
                        fieldDoc.constantValue = (Serializable) ve.getConstantValue();
                        fieldDoc.modifiers.addAll(ve.getModifiers()); 
                        DocCommentTree dct = treeUtils.getDocCommentTree(ve);
                        setDeprecationStatus(fieldDoc, ve, dct);
                        if (dct != null) {
                            fieldDoc.setFirstSentence(dct.getFirstSentence());
                            fieldDoc.setBody(dct.getBody());
                            fieldDoc.setFullBody(dct.getFullBody());
                        }
                        classDoc.fields.add(fieldDoc);
                    }
                }
            }
        }
        return super.visitVariable(ve, depth);
    }

    @Override
    public Void visitTypeParameter(TypeParameterElement e, Integer depth) {
        return scan(e.getEnclosedElements(), depth);
    }

    @Override
    public Void visitRecordComponent(RecordComponentElement e, Integer depth) {
        return visitUnknown(e, depth);
    }

    private void addConstantFieldValuesReference() {
        for (ClassNode classNode : api.getClasses()) {
            for (FieldNode fieldNode : classNode.fields) {
                if (fieldNode.constantValue != null) {
                    Reference ref = new Reference(Reference.Kind.PAGE, "Constant Field Values", "constant-values.md");
                    fieldNode.getReferences().add(ref);
                    api.getConstantValues().add(fieldNode);
                }
            }
        }
    }

    public boolean isInterface(TypeMirror typeMirror) {
        Element element = typeUtils.asElement(typeMirror);
        if (element == null) return false;
        if (element instanceof TypeElement elem) {
            ElementKind kind = elem.getKind();
            return kind.isInterface();
        }
        return false;
    }

    private TypeNode insantiateSubtype(ElementKind kind, String qualifiedName, String simpleName, String packageName) {
        switch (kind) {
            case ElementKind.CLASS:
                return new ClassNode(qualifiedName, simpleName, packageName);
            case ElementKind.INTERFACE:
                return new InterfaceNode(qualifiedName, simpleName, packageName);
            case ElementKind.ENUM:
                return new EnumNode(qualifiedName, simpleName, packageName);
            case ElementKind.ANNOTATION_TYPE:
                return new AnnotationNode(qualifiedName, simpleName, packageName);
            default:
                return null;
        }
    }

    private void findImplementedInterfaces(TypeElement typeElement, List<String> result) {
        List<? extends TypeMirror> interfaces = typeElement.getInterfaces();
        for (TypeMirror interfaceType : interfaces) {
            result.add(interfaceType.toString());
        }
    }

    private void collectAllSupertypes(TypeMirror t, List<String> result) {
        for (TypeMirror s : typeUtils.directSupertypes(t)) {
            if (s.getKind() == TypeKind.DECLARED) {
                encounteredSupertypes.add(((DeclaredType) s).asElement());
            }
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

    private DeprecatedTree getDeprecation(DocCommentTree docComment) {
        if (docComment == null) return null;
        for (DocTree docTree : docComment.getBlockTags()) {
            if (docTree instanceof DeprecatedTree tree) {
                return tree;
            }
        }
        return null;
    }        

    private ReturnTree getReturnTree(DocCommentTree dcTree) {
        if (dcTree == null) return null;
        for (DocTree docTree : dcTree.getBlockTags()) {
            if (docTree instanceof ReturnTree tree) {
                return tree;
                //.getDescription().toString();
                //TODO: Handle arrays
            }
        }
        return null;
    }        

    private ParamTree getParamTree(DocCommentTree dcTree, VariableElement parameter) {
        if (dcTree == null) return null;
        for (DocTree tagTree : dcTree.getBlockTags()) {
            if (tagTree instanceof ParamTree tree && tree.getName().toString().equals(parameter.getSimpleName().toString())) {
                return tree;
            }
        }
        return null;
    }

    private List<Reference> getReferences(DocCommentTree dcTree) {
        if (dcTree == null) return null;
        List<Reference> refs = new ArrayList<>();
        for (DocTree tagTree : dcTree.getBlockTags()) {
            if (tagTree instanceof SeeTree seeTree) {
                List<? extends DocTree> see = seeTree.getReference();
                for (DocTree docRef : see) {
                    if (docRef.getKind() == Kind.MARKDOWN) {
                        // It's actually HTML, not Markdown.
                        Reference ref = new Reference();
                        ref.kind = Reference.Kind.URL;
                        ref.uri = getUrl(docRef.toString());
                        refs.add(ref);
                    } if (docRef.getKind() == Kind.REFERENCE) {
                        Reference ref = new Reference();
                        ref.kind = Reference.Kind.TYPE;
                        ref.name = docRef.toString();
                        refs.add(ref);
                    } else if (docRef.getKind() == Kind.MARKDOWN) {
                        // Do nothing for now
                    } else {
                        System.out.println("Unhandled reference type: " + docRef.getKind().toString());
                    }
                }
            } else if (tagTree instanceof ErroneousTree) {
                System.out.println("Erroneous tag: " + tagTree.toString());
            }
        }
        return refs;
    }

    private Text getSince(DocCommentTree dcTree) {
        if (dcTree == null) return null;
        for (DocTree tagTree : dcTree.getBlockTags()) {
            if (tagTree instanceof SinceTree sinceTree) {
                return new Text(sinceTree.getBody());
            } else if (tagTree instanceof ErroneousTree) {
                System.out.println("Erroneous tag: " + tagTree.toString());
            }
        }
        return Text.empty();
    }

    private static String getUrl(String html) {
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

    private void setDeprecationStatus(Node node, Element e, DocCommentTree dct) {
        DeprecatedTree deprecatedTree = getDeprecation(dct);
        Deprecated deprecatedAnnotation = e.getAnnotation(Deprecated.class);
        node.deprecation = Deprecation.NONE;
        if (deprecatedTree != null) {
            node.deprecation = Deprecation.DEPRECATED;
            node.deprecationText.set(deprecatedTree.getBody());
        }
        if (deprecatedAnnotation != null) {
            if (deprecatedAnnotation.forRemoval()) {
                node.deprecation = Deprecation.FOR_REMOVAL;
            }else{
                node.deprecation = Deprecation.DEPRECATED;
            }
        }
    }

    private boolean isIncludedInApi(Element e) {
        Set<Modifier> mods = e.getModifiers();
        return documentPrivateMembers || mods.contains(PUBLIC) || mods.contains(PROTECTED);
    }

    private void setMethodParams(MethodNode methodDoc, ExecutableElement ee) {
        methodDoc.params.clear();
        TypeElement classElement = getEnclosingTypeElement(ee);
        DocCommentTree dct = treeUtils.getDocCommentTree(ee);
        for (VariableElement parameter : ee.getParameters()) {
            String simpleName = parameter.getSimpleName().toString();
            String qualifiedClassName = classElement.getQualifiedName().toString();
            TypeNode paramType = getParamType(elementUtils, qualifiedClassName, ee, simpleName);
            ParamNode param = new ParamNode(paramType, simpleName);
            // paramType.arrayBrackets = arrayBrackets;

            ParamTree paramTree = getParamTree(dct, parameter);
            if (paramTree != null) {
                param.setBody(paramTree.getDescription());
            }
            methodDoc.params.add(param);
        }
    }

    /// To be removed
    /// @deprecated use [new()](#new()) instead.  
    public static VariableTree getVariableTree(List<? extends VariableTree> paramList, String paramName) {
        for (VariableTree varTree : paramList) {
            if (varTree.getName().toString().equals(paramName)) {
                return varTree;
            }
        }
        return null;
    }

    private static TypeNode getFieldType(Elements elementUtils, String className, String fieldName) {
        String qualifiedTypeName = null;
        String arrayBrackets = "";
        TypeElement classElement = elementUtils.getTypeElement(className);
        for (VariableElement field : ElementFilter.fieldsIn(classElement.getEnclosedElements())) {
            if (field.getSimpleName().toString().equals(fieldName)) {
                TypeMirror typeMirror = field.asType();
                if (typeMirror.getKind() == TypeKind.ARRAY){
                    TypeMirror array = ((ArrayType)typeMirror).getComponentType();
                    qualifiedTypeName = array.toString();
                    arrayBrackets = "\\[]";                    
                    break;
                }
                qualifiedTypeName = typeMirror.toString();
                break;
            }
        }
        if (qualifiedTypeName == null) return null;

        String simpleTypeName = NameUtils.simplifyNames(qualifiedTypeName);
        String packageName = getPackageName(qualifiedTypeName);
        TypeNode type = new TypeNode(qualifiedTypeName, simpleTypeName, packageName);
        type.arrayBrackets = arrayBrackets;

        return type;
    }

    private static TypeNode getParamType(Elements elementUtils, String className, ExecutableElement method, String fieldName) {
        String qualifiedTypeName = null;
        String arrayBrackets = "";
        for (VariableElement param : method.getParameters()) {
            if (param.getSimpleName().toString().equals(fieldName)){
                TypeMirror typeMirror = param.asType();
                if (typeMirror.getKind() == TypeKind.ARRAY){
                    TypeMirror array = ((ArrayType)typeMirror).getComponentType();
                    qualifiedTypeName = array.toString();
                    arrayBrackets = "\\[]";
                    
                    break;
                }
                qualifiedTypeName = typeMirror.toString();
                break;
            }
        }
        if (qualifiedTypeName == null) return null;

        String simpleTypeName = NameUtils.simplifyNames(qualifiedTypeName);
        String packageName = getPackageName(qualifiedTypeName);
        TypeNode type = new TypeNode(qualifiedTypeName, simpleTypeName, packageName);
        type.arrayBrackets = arrayBrackets;

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

    private static String getSimpleName(String qualifiedTypeName) {
        if (qualifiedTypeName == null) return null;
        String simpleName = qualifiedTypeName;
        if (simpleName.indexOf(".")>-1) {
            simpleName = simpleName.substring(simpleName.lastIndexOf(".") + 1);
        }
        return simpleName;
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
