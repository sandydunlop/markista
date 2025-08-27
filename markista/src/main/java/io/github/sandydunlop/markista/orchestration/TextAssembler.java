package io.github.sandydunlop.markista.orchestration;

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
                    .from(ctx.getPackageName())
                    .withKind(Link.Kind.TYPE)
                    .withLabel(ownerTypeNode.getQualifiedName());
            LinkResolver.resolve(ref);
            typeNode.setEnclosingClassRef(ref);
        }
        for (TypeReference typeRef : typeNode.getImplementedInterfaces()) {
            Text text = link(typeRef.getLink());
            typeRef.getText().append(text);
        }
        for (TypeReference typeRef : typeNode.getSupertypes()) {
            Text text = link(typeRef.getLink());
            typeRef.getText().append(text);
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
                Text subtypeText = link(subtypeRef.getLink());
                subtypeRef.getLink().from(directSupertype.getQualifiedName());
                subtypeRef.setText(subtypeText);
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
            link(methodRef);
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
                supertypeRef.setText(link(supertypeRef.getLink()));
                typeNode.getInheritedMethods().put(supertypeRef, methods);
            }            
        }
    }

    public static void processMethod(MethodNode method) {
        Link returnTypeReference = Link.to(method.getReturnTypeName()).from(ctx.getPackageName());
        method.setReturnTypeText(link(returnTypeReference));
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
        Link baseMethodPair = method.getBaseMethod();
        if (baseMethodPair != null) {
            String baseTypeName = baseTypeName(method);
            if (baseTypeName != null) {
                linkBaseMethod(baseMethodPair, baseTypeName);
            } else {
                //TODO: We arrive here when the base type is not in the model - eg a standard java type
                method.setBaseMethod(null);
            }

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
        link(baseMethodLink);
        baseMethodLink.setLabel(baseMethodLink.getSimpleClassName() + "." + baseMethodLink.getMethodName());
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
            TypeNode supertypeNode = api.getTypeNode(typeRef.getLink().getTarget());
            if (supertypeNode != null && typeHasMethod(supertypeNode, method)) {
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
                Link ref = Link.to(constant.getTypeName())
                        .from("")
                        .withLabel(constant.getTypeName());
                LinkResolver.resolve(ref);
                constant.setConstantValueReference(ref);
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
            String interfaceName = interfaceRef.getLink().getTarget();
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
        String simpleName = interfaceRef.getLink().getSimpleClassName().replace(".", "$");
        String qualifiedName = interfaceRef.getLink().getPackageName() + "." + simpleName;
        ClassLoader classLoader = TextAssembler.class.getClassLoader();
        try {
            Class<?> standardClass = classLoader.loadClass(qualifiedName);
            Method[] methods = standardClass.getMethods();
            InterfaceNode interfaceNode = new InterfaceNode(qualifiedName, standardClass.getPackageName());
            interfaceNode.setQualifiedName(qualifiedName);
            for (Method method : methods) {
                MethodNode methodNode = new MethodNode("", method.getName());
                interfaceNode.addMethod(methodNode);
            }
            return interfaceNode;
        } catch (ClassNotFoundException _) {
            return null;
        }
    }

    public static void associateClassWithInterface(InterfaceNode interfaceNode, TypeNode typeNode) {
        Link implementingClassLink = Link
                .to(typeNode.getQualifiedName())
                .from(interfaceNode.getQualifiedName())
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
                            .from(typeNode.getQualifiedName())
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
            ctx.setPackageName(link.getOrigin());
            LinkResolver.resolve(link);
            if (link.getLabel().contains(".")) {
                link.setLabel(Utils.simplifyNames(link.getLabel()));
            }
        }
    }

    static void generateLinkTextsForParams(List<ParamNode> params) {
        for (ParamNode param : params) {
            Link reference = Link.to(param.getTypeName()).from(ctx.getPackageName());
            param.setTypeText(link(reference));
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

    /// Create a markdown link, automatically deciding what kind of link to make.
    /// @param reference a Reference object describing the link
    /// @return markdown formatted link
    public static Text link(Link reference) {
        String targetName = reference.getTarget();
        if (targetName == null || targetName.isEmpty()) {
            ctx.reportWarning("No link target supplied");
            return Text.of(reference.getLabel());
        }
        String pre = "";
        String post = "";
        String anchor = "";
        boolean isLocalMethod = false;
        String displayName = reference.getLabel();
        int pos = targetName.indexOf('#');
        if (pos == 0) {
            reference.setHasAnchor(true);
            reference.setAnchor(targetName.substring(pos));
            Text.Segment segment = Text.Segment.empty()
                    .setKind(Text.Segment.Kind.LINK)
                    .setLink(reference)
                    .setText(reference.getLabel());
            return Text.of(segment);
        }
        if (pos > 0) {
            anchor = targetName.substring(pos);
            targetName = targetName.substring(0, pos);
        }
        if (targetName.indexOf('<') > -1) {
            // Does escaping need done here?
            // See: https://sandydunlop.atlassian.net/browse/MFLP-57
            return linkGenerics(targetName);
        } else if (targetName.indexOf(',') > -1) {
            return splitAndLink(targetName);
        } else if (targetName.lastIndexOf(' ') > 0) {
            int p = targetName.lastIndexOf(' ');
            pre = targetName.substring(0, p) + " ";
            targetName = targetName.substring(p + 1);
        }

        pos = targetName.indexOf('[');
        if (pos > 0) {
            post = targetName.substring(pos);
            targetName = targetName.substring(0, pos);
        }
        if (targetName.contains("(")) {
            targetName = targetName.substring(0, targetName.indexOf("("));
        }
        reference.setTarget(targetName);
        reference.setAnchor(anchor);
        if (reference.getLabel() == null || reference.getLabel().isEmpty()) {
            reference.setLabel(reference.getTarget());
        }
        LinkResolver.resolve(reference);
        if (!anchor.isEmpty() && reference.getKind() != Link.Kind.URL) {
            isLocalMethod = true;
            // Issue: https://github.com/sandydunlop/markista/issues/1
            // Workaround:
            // Remove the parentheses from after method names in anchor 
            // links to Markdown pages for now. Anchors in the Markdown
            // are currently headings without parameters.
            reference.setAnchor(Utils.removeParentheses(reference.getAnchor()));
        }
        setDisplayName(reference, displayName, isLocalMethod);

        Text.Segment link = Text.Segment.empty()
                .setKind(Text.Segment.Kind.LINK)
                .setLink(reference)
                .setText(Utils.simplifyNames(reference.getLabel()));
        Text r = Text.empty();
        r.append(pre);
        r.append(link);
        r.append(post);
        return r;
    }

    public static void setDisplayName(Link reference, String displayName, boolean isLocalMethod) {
        if (reference.getLabel() == null) {
             reference.setLabel(reference.getTarget());
        }
        if (isLocalMethod) {
            reference.setKind(Link.Kind.METHOD);
            String methodName = reference.getAnchor().substring(1);
            reference.setMethodName(methodName);
            if (!reference.getQualifiedClassName().equals(ctx.getTypeName())) {
                reference.setLabel(reference.getLabel() + "." + methodName);
            } else {
                reference.setLabel(methodName);
            }
        }
        if (displayName != null && !displayName.isEmpty()) {
            reference.setLabel(displayName.replace("#","."));
        }
        reference.setLabel(escape(reference.getLabel()));
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

    /// Changes qualified generic type names to unqualified generic 
    /// type names and adds links to their API documentation.
    /// @param str A string containing a qualified generic name.
    /// @return    A Text object with the qualified names changed to unqualified
    ///            names and links to types added
    public static Text linkGenerics(String str) {
        if (str == null || str.isEmpty()) return Text.of(str);
        int openingChevron = str.indexOf("<");
        int closingChevron = str.lastIndexOf(">");
        String before = str.substring(0, openingChevron);
        String mid = str.substring(openingChevron + 1, closingChevron);
        String after = str.substring(closingChevron + 1);
        Text typeLink = link(Link.to(before));

        Text midLinks;
        if (mid.contains("<")) {
            midLinks = linkGenerics(mid);
        } else {
            midLinks = splitAndLink(mid);
        }

        Text ret = Text.empty();
        ret.append(typeLink);
        ret.append("<");
        ret.append(midLinks);
        ret.append(">");
        ret.append(Text.of(after));
        return ret;
    }

    /// Creates markdown formatted text with links to types from a string.
    /// containing one or more types separated by commas.
    /// @param typesString A string containing a comma-separated list of type names
    /// @return a list of links to types formatted as Markdown
    public static Text splitAndLink(String typesString) {
        Text text = Text.empty();
        String[] types = typesString.split(",");
        for (String t : types) {
            String typeName = t.strip();
            if (!text.isEmpty()) {
                text.append(", ");
            }
            text.append(link(Link.to(typeName)));
        }
        return text;
    }

    /// Removes the generic type and its surrounding <> from a string, if present
    /// @param str The string
    /// @return The string with the generic type and surrounding <> removed
    public static String removeGenerics(String str) {
        if (str == null || str.isEmpty()) return "";
        int start = str.indexOf("<");
        if (start > -1) {
            int end = str.indexOf(">");
            if (end > start) {
                return str.substring(0, start);
            }
        }
        return str;
    }
}
