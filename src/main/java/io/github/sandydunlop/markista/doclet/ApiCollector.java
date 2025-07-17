package io.github.sandydunlop.markista.doclet;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTree.Kind;
import com.sun.source.doctree.ErroneousTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.doctree.SinceTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTreeFactory;
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
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.Reference;
import io.github.sandydunlop.markista.model.Text;
import io.github.sandydunlop.markista.model.TypeNode;
import io.github.sandydunlop.markista.util.LinkResolver;
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
import javax.lang.model.element.PackageElement;
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
        this.typeUtils = environment.getTypeUtils();
        this.treeUtils = environment.getDocTrees();
        this.elementUtils = environment.getElementUtils();
        this.api = new Api();
    }

    public void setDocumentPrivateMembers(boolean documentPrivateMembers) {
        this.documentPrivateMembers = documentPrivateMembers;
    }

    public Api collect(Set<? extends Element> elements) {
        scan(elements, 0);
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
            LinkResolver.addLocalPackage(pkg.qualifiedName);
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
                //TODO: @see, @since
                method.setFirstSentence(dct.getFirstSentence());
                method.setBody(dct.getBody());
                method.setFullBody(dct.getFullBody());
                method.returnDescription = getReturnComment(dct);
                method.setReferences(getReferences(dct));
                method.since = getSince(dct);
            }
            
            method.thrownTypes = ee.getThrownTypes(); //TODO convert to model.* types
            method.deprecation = getDeprecationStatus(ee);
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
                        String qualifiedTypeName = getFieldType(elementUtils, qualifiedClassName, simpleName);
                        String simpleTypeName = NameUtils.simplifyNames(qualifiedTypeName);
                        String packageName = getPackageName(qualifiedTypeName);
                        TypeNode type = new TypeNode(qualifiedTypeName, simpleTypeName, packageName);
                        fieldDoc = new FieldNode(type, simpleName);
                        fieldDoc.constantValue = (Serializable) ve.getConstantValue();
                        fieldDoc.deprecation = getDeprecationStatus(ve);
                        fieldDoc.modifiers.addAll(ve.getModifiers()); 
                        DocCommentTree dct = treeUtils.getDocCommentTree(ve);
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

    public boolean isInterface(TypeMirror typeMirror) {
        Element element = typeUtils.asElement(typeMirror);
        if (element == null) return false;
        if (element instanceof TypeElement elem) {
            ElementKind kind = elem.getKind();
            return kind.isInterface();
        }
        return false;
        // return element instanceof TypeElement && ((TypeElement) element).getKind().isInterface();
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

    private String getReturnComment(DocCommentTree docComment) {
        for (DocTree docTree : docComment.getBlockTags()) {
            if (docTree instanceof ReturnTree returnTree) {
                return returnTree.getDescription().toString();
                //TODO: Handle arrays
            }
        }
        return "";
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
                        ref.url = getUrl(docRef.toString());
                        refs.add(ref);
                    } if (docRef.getKind() == Kind.REFERENCE) {
                        Reference ref = new Reference();
                        ref.kind = Reference.Kind.TYPE;
                        ref.typeName = docRef.toString();
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

    private Deprecation getDeprecationStatus(Element e) {
        Deprecated a = e.getAnnotation(Deprecated.class);
        return a == null ? Deprecation.NONE : a.forRemoval() ? Deprecation.FOR_REMOVAL : Deprecation.DEPRECATED;
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
            String qualifiedTypeName = getParamType(elementUtils, qualifiedClassName, ee, simpleName);
            String packageName = getPackageName(qualifiedTypeName);
            Element typeElement = typeUtils.asElement(parameter.asType());
            String simpleTypeName = typeElement == null ? qualifiedTypeName : typeElement.getSimpleName().toString();

            TypeMirror type = parameter.asType();
            if (type.getKind() == TypeKind.ARRAY) {
                simpleTypeName = getSimpleName(((ArrayType)type).getComponentType().toString()) + "[]";
            }

            TypeNode paramType = new TypeNode(qualifiedTypeName, simpleTypeName, packageName);
            ParamNode param = new ParamNode(paramType, simpleName);

            ParamTree paramTree = getParamTree(dct, parameter);
            if (paramTree != null) {
                param.setBody(paramTree.getDescription());
            }
            methodDoc.params.add(param);
        }
    }

    private static VariableTree getVariableTree(List<? extends VariableTree> paramList, String paramName) {
        for (VariableTree varTree : paramList) {
            if (varTree.getName().toString().equals(paramName)) {
                return varTree;
            }
        }
        return null;
    }

    private static String getFieldType(Elements elementUtils, String className, String fieldName) {
        TypeElement classElement = elementUtils.getTypeElement(className);
        for (VariableElement field : ElementFilter.fieldsIn(classElement.getEnclosedElements())) {
            if (field.getSimpleName().toString().equals(fieldName)) {
                TypeMirror type = field.asType();
                if (type.getKind() == TypeKind.ARRAY){
                    TypeMirror array = ((ArrayType)type).getComponentType();
                    return array.toString() + "\\[]";
                }
                return type.toString();
            }
        }
        return null;
    }

    private static String getParamType(Elements elementUtils, String className, ExecutableElement method, String fieldName) {
        for (VariableElement param : method.getParameters()) {
            if (param.getSimpleName().toString().equals(fieldName)){
                TypeMirror type = param.asType();
                if (type.getKind() == TypeKind.ARRAY){
                    TypeMirror array = ((ArrayType)type).getComponentType();
                    return array.toString() + "\\[]";
                }
                return type.toString();
            }
        }
        return null;
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
