package io.github.sandydunlop.markista.orchestration;

import io.github.sandydunlop.markista.common.JreTools;
import io.github.sandydunlop.markista.common.Utils;
import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.DirectiveNode;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.InterfaceNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.Node;
import io.github.sandydunlop.markista.model.Pair;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.RecordNode;
import io.github.sandydunlop.markista.model.Link;
import io.github.sandydunlop.markista.model.Text;
import io.github.sandydunlop.markista.model.TypeNode;
import io.github.sandydunlop.markista.model.TypeReference;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TextAssembler {
    private static Context ctx;
    private static Api api;

    private TextAssembler() {
        // Nothing to see here
    }

    /// Generates [Text] objects for links to types and links in Javadoc text.
    /// This is where we decide if the label for those links shows qualified names or simplified names.
    /// @param a The API model
    /// @param c The doclet context to keep track of why package and type are being processed
    public static void assembleTextAndLinks(Api a, Context c) {
        api = a;
        ctx = c;

        for (MethodNode method : api.getMethods()) {
            associateMethodWithType(method);
        }

        processModules(api.getModules());

        // Links from types used in methods and fields
        for (TypeNode typeNode : api.getTypes()) {
            processTypeNode(typeNode);
            associateMethodsWithImplementedInterfaces(typeNode);
        }

        processJavadocComments(api);
        addJavadocToRecords(api);
    }

    public static void addJavadocToRecords(Api api) {
        for (TypeNode recordView : api.getRecords()) {
            RecordNode recordNode = (RecordNode) recordView;
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

    public static void processTypeNode(TypeNode typeNode) {
        ctx.setPackageName(typeNode.getPackageName());
        ctx.setTypeName(typeNode.getQualifiedName());

        // Types
        TypeNode ownerTypeNode = api.getTypeNode(typeNode.getOwnerName());
        if (ownerTypeNode != null) {
            Link ref = Link.to(ownerTypeNode.getQualifiedName())
                    .fromPackage(ctx.getPackageName())
                    .withKind(Link.Kind.TYPE)
                    .withLabel(ownerTypeNode.getQualifiedName());
            LinkResolver.resolve(ref);
            typeNode.setEnclosingClassRef(ref);
        }
        for (TypeReference typeRef : typeNode.getImplementedInterfaces()) {
            resolveTypeRererence(typeRef);
        }
        for (TypeReference typeRef : typeNode.getSupertypes()) {
            resolveTypeRererence(typeRef);
        }
        processSubtypes(typeNode);
        processInheritedMethods(typeNode);
        generateLinkTextsForReferences(typeNode);

        // Fields
        generateLinkTextsForParams(typeNode.getFields()
                .stream()
                .filter(ParamNode.class::isInstance)
                .map(ParamNode.class::cast)
                .toList());
        // Methods
        for (MethodNode method : typeNode.getMethods()) {
            processMethod(method);
        }
        for (MethodNode method : typeNode.getConstructors()) {
            processMethod(method);
        }
    }

    public static void processSubtypes(TypeNode typeNode) {
        if (typeNode.getSupertypes().size() > 1) {
            TypeReference typeRef = typeNode.getSupertypes().getLast();
            String directSupertypeName = typeRef.getLink().getTarget();
            TypeNode directSupertype = api.getTypeNode(directSupertypeName);
            if (directSupertype != null) {
                TypeReference subtypeRef = TypeReference.to(typeNode.getQualifiedName());
                subtypeRef.getLink().fromPackage(directSupertype.getQualifiedName());
                resolveTypeRererence(subtypeRef);
                directSupertype.getSubtypes().add(subtypeRef);
            }
        }
    }

    public static Map<String,Pair<String,Link>> gatherOverriddenMethods(TypeNode typeNode) {
        HashMap<String,Pair<String,Link>> methodLookup1 = new HashMap<>();
        // Skip the first one (java.lang.Object)
        for (int i = 1; i < typeNode.getSupertypes().size() ; i++) {
            String supertypeName = typeNode.getSupertypes().get(i).getQualifiedName();
            TypeNode supertype = api.getTypeNode(supertypeName);
            if (supertype != null) {
                for (MethodNode baseMethod : supertype.getMethods()) {
                    if (!typeHasMethod(typeNode, baseMethod)) {
                        Link methodRef = Link.to(supertypeName + "#" + baseMethod.signature());
                        methodRef.setLabel(baseMethod.getSimpleName());
                        Pair<String,Link> refs = Pair.of(supertypeName, methodRef);
                        methodLookup1.put(baseMethod.signature(), refs);
                    }
                }
            }
        }
        return methodLookup1;
    }

    public static Map<String,List<Link>> listBySupertypeName(Map<String,Pair<String,Link>> methodLookup1) {
        HashMap<String,List<Link>> methodLookup2 = new HashMap<>();
        for (Map.Entry<String,Pair<String,Link>> entry : methodLookup1.entrySet()) {
            Pair<String,Link> refs = entry.getValue();
            String supertypeName = refs.getL();
            Link methodRef = refs.getR();
            resolveLink(methodRef);
            List<Link> inheritedMethods = methodLookup2.get(supertypeName);
            if (inheritedMethods == null) {
                List<Link> newList = new ArrayList<>();
                newList.add(methodRef);
                methodLookup2.put(supertypeName, newList);
            } else {
                methodLookup2.get(supertypeName).add(methodRef);
            }
        }
        return methodLookup2;
    }

    public static void processInheritedMethods(TypeNode typeNode) {
        if (typeNode.getSupertypes().size() > 1) {
            Map<String, Pair<String, Link>> overriddenMethods = gatherOverriddenMethods(typeNode);
            Map<String,List<Link>> methodLookup2 = listBySupertypeName(overriddenMethods);
            // Copy methodLookup2 into typeNode's inheritedMethods hash table
            // but use a TypeReference as the key
            for (Map.Entry<String,List<Link>> entry : methodLookup2.entrySet()) {
                List<Link> methods = entry.getValue();
                TypeReference supertypeRef = TypeReference.to(entry.getKey());
                resolveTypeRererence(supertypeRef);
                typeNode.getInheritedMethods().put(supertypeRef, methods);
            }
        }
    }

    public static void processMethod(MethodNode method) {
        resolveTypeRererence(method.getReturnType());
        generateLinkTextsForParams(method.getParams()
                .stream()
                .filter(ParamNode.class::isInstance)
                .map(ParamNode.class::cast)
                .toList());
        if (method.getSpecifiedBy() != null && !method.getSpecifiedBy().getTarget().isEmpty()) {
            LinkResolver.resolve(method.getSpecifiedBy());
        }
        for (Link thrownRef : method.getThrownTypes()) {
            LinkResolver.resolve(thrownRef);
        }
        Link baseMethodRef = method.getBaseMethod();
        if (baseMethodRef != null) {
            String baseTypeName = baseTypeName(method);
            if (baseTypeName != null) {
                linkBaseMethod(baseMethodRef, baseTypeName);
            } else {
                // Arriving here would indicate that the class is not in the model and not a standard java class
                method.setBaseMethod(null);
            }

            // Now process @inheritDocs
            TypeNode baseType = api.getTypeNode(baseTypeName);
            if (baseType != null) {
                MethodNode baseMethod = baseType.getMethod(method);
                if (baseMethod != null) {
                    method.setFirstSentence(processInheritDocTags(baseMethod.getFirstSentence(), method.getFirstSentence()));
                    method.setBody(processInheritDocTags(baseMethod.getBody(), method.getBody()));
                    method.setFullBody(processInheritDocTags(baseMethod.getFullBody(), method.getFullBody()));
                }
            }
        }
        generateLinkTextsForReferences(method);
    }

    public static void linkBaseMethod(Link baseMethodLink, String baseTypeName) {
        if (baseMethodLink.getTarget().isEmpty()) {
            if (!baseMethodLink.getMethodSignature().isEmpty()) {
                baseMethodLink.setTarget(baseTypeName + "#" + baseMethodLink.getMethodSignature());
            } else if (!baseMethodLink.getMethodName().isEmpty()) {
                baseMethodLink.setTarget(baseTypeName + "#" + baseMethodLink.getMethodName());
            }
        }
        baseMethodLink.setKind(Link.Kind.UNKNOWN);
        baseMethodLink.setLabel(baseMethodLink.getSimpleClassName() + "." + baseMethodLink.getMethodName());
        resolveLink(baseMethodLink);
    }

    public static Text processInheritDocTags(Text baseMethodText, Text text) {
        int segmentCount = text.getSegments().size();
        for (int i = segmentCount - 1; i >= 0; i--) {
            Text.Segment segment = text.getSegments().get(i);
            if (segment.getKind() == Text.Segment.Kind.INHERIT) {
                Text before = text.subtext(0, i - 1);
                Text after = text.subtext(i + 1);
                Text inherited = baseMethodText;
                text = before.append(inherited).append(after);
            }
        }
        return text;
    }

    public static String baseTypeName(MethodNode method){
        TypeNode type = api.getTypeNode(method.getOwnerName());
        if (type == null) {
            return null;
        }
        for (int i = type.getSupertypes().size() - 1; i >= 0; i--) {
            TypeReference typeRef = type.getSupertypes().get(i);
            String typeName = typeRef.getTypeString();
            TypeNode supertypeNode = api.getTypeNode(typeName);
            if (supertypeNode == null) {
                // It's not in the model, try to find in JRE
                Class<?> standardClass = JreTools.loadClass(typeName);
                if (standardClass != null && JreTools.typeHasMethod(standardClass, method.signature())) {
                    return standardClass.getCanonicalName();
                }
            } else if (typeHasMethod(supertypeNode, method)) {
                return supertypeNode.getQualifiedName();
            }
        }
        return null;
    }

    public static boolean typeHasMethod(TypeNode typeNode, MethodNode methodNode) {
        String methodSignature = methodNode.signature();
        for (MethodNode inheritedMethod : typeNode.getMethods()) {
            if (inheritedMethod.signature().equals(methodSignature)) {
                return true;
            }
        }
        return false;
    }

    public static void processModules(List<ModuleNode> modules) {
        for (ModuleNode module : modules) {
            ctx.setModuleName(module.getName());
            // Constant field values
            for (FieldNode constant : module.getConstantValues()) {
                resolveTypeRererence(constant.getType());
                constant.setConstantValueReference(constant.getType());
            }

            // Directives
            for (DirectiveNode directive : module.getDirectives()) {
                LinkResolver.resolve(directive.getReference());
                LinkResolver.resolve(directive.getInterface());
                for (Link implementation : directive.getImplementations()) {
                    LinkResolver.resolve(implementation);
                }
                for (Link pkg : directive.getPackages()) {
                    LinkResolver.resolve(pkg);
                }
            }
        }
    }

    public static void associateMethodWithType(MethodNode methodNode) {
        ctx.setTypeName(methodNode.getOwnerName());
        ctx.setMethodName(methodNode.getSimpleName());
        TypeNode ownerType = api.getTypeNode(methodNode.getOwnerName());
        if (ownerType == null) {
            ctx.reportError("Unable to determine owner of method");
            return;
        }

        if (methodNode.isConstructor()) {
            methodNode.setSimpleName(ownerType.getSimpleName());
            MethodNode existingMethodNode = ownerType.getConstructor(methodNode);
            if (existingMethodNode == null) {
                ownerType.addConstructor(methodNode);
            }
        } else {
            MethodNode existingMethodNode = ownerType.getMethod(methodNode);
            if (existingMethodNode == null) {
                ownerType.getMethods().add(methodNode);
            }
        }
    }

    public static void associateMethodsWithImplementedInterfaces(TypeNode typeNode) {
        ctx.setPackageName(typeNode.getPackageName());
        ctx.setTypeName(typeNode.getQualifiedName());
        List<TypeReference> interfaces = typeNode.getImplementedInterfaces();
        for (TypeReference interfaceRef : interfaces) {
            String interfaceName = interfaceRef.getTypeString();
            InterfaceNode interfaceType = (InterfaceNode) api.getTypeNode(interfaceName);
            if (interfaceType == null) {
                interfaceType = getStandardInterface(interfaceRef);
            }
            if (interfaceType == null) {
                ctx.reportError("Can't find interface: " + interfaceName);
                return;
            }
            associateClassWithInterface(interfaceType, typeNode);
            associateMethodsWithInterface(interfaceType, typeNode);
        }
    }

    public static InterfaceNode getStandardInterface(TypeReference interfaceRef) {
        String qualifiedName = interfaceRef.getTypeString();
        Class<?> standardClass = JreTools.loadClass(qualifiedName);
        if (standardClass == null) {
            return null;
        }
        Method[] methods = standardClass.getMethods();
        InterfaceNode interfaceNode = new InterfaceNode(qualifiedName, standardClass.getPackageName());
        interfaceNode.setQualifiedName(qualifiedName);
        for (Method method : methods) {
            MethodNode methodNode = new MethodNode("", method.getName());
            interfaceNode.addMethod(methodNode);
        }
        return interfaceNode;
    }

    public static void associateClassWithInterface(InterfaceNode interfaceNode, TypeNode typeNode) {
        Link implementingClassLink = Link
                .to(typeNode.getQualifiedName())
                .fromPackage(interfaceNode.getQualifiedName())
                .withLabel(typeNode.getSimpleName());
        LinkResolver.resolve(implementingClassLink);
        interfaceNode.addImplementingClass(implementingClassLink);
    }

    public static void associateMethodsWithInterface(InterfaceNode interfaceNode, TypeNode typeNode) {
        for (MethodNode methodNode : typeNode.getMethods()) {
            for (MethodNode interfaceMethod : interfaceNode.getMethods()) {
                if (interfaceMethod.signature().equals(methodNode.signature())) {
                    Link specifiedByLink = Link
                            .to(interfaceNode.getQualifiedName())
                            .fromPackage(typeNode.getQualifiedName())
                            .withLabel(interfaceNode.getSimpleName());
                    LinkResolver.resolve(specifiedByLink);
                    methodNode.setSpecifiedBy(specifiedByLink);
                    break;
                }
            }
        }
    }

    public static void processJavadocComments(Api api) {
        for (Link link : api.getLinks()) {
            ctx.setPackageName(link.getOriginPackage());
            resolveLink(link);
            if (link.getLabel().contains(".")) {
                link.setLabel(Context.NameSimplifier.simplifyNames(link.getLabel()));
            }
        }
    }

    static void generateLinkTextsForParams(List<ParamNode> params) {
        for (ParamNode param : params) {
            resolveTypeRererence(param.getType());
            generateLinkTextsForReferences(param);
        }
    }

    public static void generateLinkTextsForReferences(Node node) {
        for (Link reference : node.getReferences()) {
            if (reference.getKind() == Link.Kind.PAGE) {
                String relativePath = LinkResolver.relativize("");
                reference.setUri(Path.of(relativePath, reference.getTarget()).toString());
            } else {
                LinkResolver.resolve(reference);
            }
        }
    }

    /// Resolve links to types referenced by a [TypeReference].
    /// @param typeRef a TypeReference object describing the links
    public static void resolveTypeRererence(TypeReference typeRef) {
        switch (typeRef) {
            case TypeReference.Generic generic -> {
                LinkResolver.resolve(generic.getLink());
                resolveTypeRererence(generic.getParams());
            }
            case TypeReference.Sequence sequence -> {
                for (TypeReference element : sequence) {
                    resolveTypeRererence(element);
                }
            }
            default -> LinkResolver.resolve(typeRef.getLink());
        }
    }

    public static void resolveLink(Link link) {
        String targetName = link.getTarget();
        if (targetName == null || targetName.isEmpty()) {
            ctx.reportWarning("No link target supplied");
            return;
        }

        int pos = targetName.indexOf('#');
        if (pos == 0) {
            link.setKind(Link.Kind.METHOD);
            link.setMethodName(targetName.substring(1));
            link.setAnchor(targetName.substring(1));
            return;
        } else if (pos > 0) {
            link.setTarget(targetName.substring(0, pos));
            link.setMethodSignature(targetName.substring(pos + 1));
            link.setAnchor(targetName.substring(pos + 1));
            pos = link.getMethodSignature().indexOf("(");
            if (pos > -1) {
                link.setMethodName(link.getMethodSignature().substring(0, pos));
            } else {
                link.setMethodName(link.getMethodSignature());
            }
        }

        pos = link.getTarget().indexOf("(");
        if (pos > -1) {
            link.setTarget(link.getTarget().substring(0, pos));
        }

        if (link.getLabel() == null || link.getLabel().isEmpty()) {
            link.setLabel(link.getTarget());
        }

        LinkResolver.resolve(link);

        if (!link.getAnchor().isEmpty() && link.getKind() != Link.Kind.URL) {
            // Issue: https://github.com/sandydunlop/markista/issues/1
            // Workaround:
            // Remove the parentheses from after method names in anchor
            // links to Markdown pages for now. Anchors in the Markdown
            // are currently headings without parameters.
            link.setKind(Link.Kind.METHOD);
            link.setAnchor(Utils.removeParentheses(link.getAnchor()));
        }
    }

    /// Escapes HTML `<` and `>` characters in a string with their corresponding
    /// HTML character entities, `&lt;` and `&gt;`.
    /// @param str A string to be escaped
    /// @return The escaped string
    public static String escape(String str) {
        return str
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
