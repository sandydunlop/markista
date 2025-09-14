package io.github.sandydunlop.markista.orchestration;

import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.DirectiveNode;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.FileLink;
import io.github.sandydunlop.markista.model.InterfaceNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.MethodReference;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.Name;
import io.github.sandydunlop.markista.model.Node;
import io.github.sandydunlop.markista.model.Pair;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.RecordNode;
import io.github.sandydunlop.markista.model.Reference;
import io.github.sandydunlop.markista.model.Link;
import io.github.sandydunlop.markista.model.Text;
import io.github.sandydunlop.markista.model.TypeNode;
import io.github.sandydunlop.markista.model.VariableType;

import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TextAssembler {
    private static Context ctx;
    private static Api api;
    private static LinkResolver resolver;

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
        ModelUtils.init(a, c);
        resolver = new LinkResolver(a, c);

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

    static void reportFailure(Link link) {
        Reference target = link.getTarget();
        String name = "";
        String kind = "unknown";
        if (target.getName() == null) {
            name = target.getModuleName();
            kind = "module";
        } else {
            name = target.getName().toString();
            if (target.getName().isPackage()) kind = "package";
            if (target.getName().isType()) kind = "type";
            if (target.getName().isMember()) kind = "member";
        }
        // This is usually a type parameter
        ctx.setSourceCodeLocation(link.getSourceCodeLocation());
        ctx.reportWarning("Failed to resolve " + kind + ": \"" + name + "\"");
    }

    public static void processModules(List<ModuleNode> modules) {
        for (ModuleNode module : modules) {
            ctx.setModuleName(module.getName());
            // Constant field values
            for (FieldNode constant : module.getConstantValues()) {
                resolver.resolveVariableType(constant.getType());
                constant.setConstantValueReference(constant.getType());
            }

            // Directives
            for (DirectiveNode directive : module.getDirectives()) {
                resolver.resolveLink(directive.getLink());
                resolver.resolveLink(directive.getInterface());
                for (Link implementation : directive.getImplementations()) {
                    resolver.resolveLink(implementation);
                }
                for (Link pkg : directive.getPackages()) {
                    resolver.resolveLink(pkg);
                }
            }
        }
    }

    public static void processTypeNode(TypeNode typeNode) {
        ctx.setPackageName(typeNode.getPackageName());
        ctx.setTypeName(typeNode.getName().fullyQualifiedName());

        // Types
        TypeNode ownerTypeNode = api.getTypeNode(typeNode.getOwnerName());
        if (ownerTypeNode != null) {
            Reference ref = new Reference(ownerTypeNode.getName().fullyQualifiedName());
            Link link = Link.to(ref).from(here());
            resolver.resolveLink(link);
            typeNode.setEnclosingClassRef(link);
        }
        for (VariableType typeRef : typeNode.getImplementedInterfaces()) {
            resolver.resolveVariableType(typeRef);
        }
        for (VariableType typeRef : typeNode.getSupertypes()) {
            resolver.resolveVariableType(typeRef);
        }
        processSubtypes(typeNode);
        processInheritedMethods(typeNode);
        resolveLinksForReferences(typeNode);

        // Fields
        resolveLinksForParams(typeNode.getFields()
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

    public static void processMethod(MethodNode method) {
        resolver.resolveVariableType(method.getReturnType());
        resolveLinksForParams(method.getParams()
                .stream()
                .filter(ParamNode.class::isInstance)
                .map(ParamNode.class::cast)
                .toList());
        if (method.getSpecifiedBy() != null && method.getSpecifiedBy().getTarget() !=null) {
            resolver.resolveLink(method.getSpecifiedBy());
        }
        for (Link thrownRef : method.getThrownTypes()) {
            resolver.resolveLink(thrownRef);
        }
        Link baseMethodLink = method.getBaseMethod();
        if (baseMethodLink != null) {
            Name name = new Name(method.getName().simpleName());
            name.setIsMember(true);
            baseMethodLink.getTarget().setName(name);
            String baseTypeName = ModelUtils.baseTypeName(method);
            if (baseTypeName != null) {
                linkBaseMethod(baseMethodLink, baseTypeName);
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
                }
            }
        }
        resolveLinksForReferences(method);
    }

    //
    //
    //

    public static void processSubtypes(TypeNode typeNode) {
        if (typeNode.getSupertypes().size() > 1) {
            VariableType typeRef = typeNode.getSupertypes().getLast();
            Link link = typeRef.getLink();
            if (link != null) {
                String directSupertypeName = link.getTarget().getName().fullyQualifiedName();
                TypeNode directSupertype = api.getTypeNode(directSupertypeName);
                if (directSupertype != null) {
                    VariableType subtypeRef = VariableType.parse(typeNode.getName().fullyQualifiedName());
                    Name fromPackageName = new Name(directSupertype.getName().fullyQualifiedName(), directSupertype.getPackageName());
                    subtypeRef.getLink().from(new Reference("", fromPackageName));
                    resolver.resolveVariableType(subtypeRef);
                    directSupertype.getSubtypes().add(subtypeRef);
                }
            }
        }
    }

    public static Map<String,Pair<String,MethodReference>> gatherOverriddenMethods(TypeNode typeNode) {
        // This is realted to ElementModeller#setMethodAnnotations and TextAssembler#linkBaseMethod
        HashMap<String,Pair<String,MethodReference>> methodLookup1 = new HashMap<>();
        // Skip the first one (java.lang.Object)
        for (int i = 1; i < typeNode.getSupertypes().size() ; i++) {
            String supertypeName = typeNode.getSupertypes().get(i).getRawTypeName();
            TypeNode supertype = api.getTypeNode(supertypeName);
            if (supertype != null) {
                for (MethodNode baseMethod : supertype.getMethods()) {
                    if (!ModelUtils.typeHasMethod(typeNode, baseMethod)) {
                        Name methodName = new Name(baseMethod.getName().simpleName(), supertypeName, supertype.getPackageName());
                        Link link = Link.to(new Reference("", methodName));
                        link.setMethodName(baseMethod.getName().simpleName());
                        link.setAnchor(baseMethod.getName().simpleName().toLowerCase());
                        MethodReference mr = new MethodReference();
                        mr.setSignature(baseMethod.simplifiedSignature());
                        mr.setLink(link);
                        Pair<String,MethodReference> refs = Pair.of(supertypeName, mr);
                        methodLookup1.put(baseMethod.simplifiedSignature(), refs);
                    }
                }
            }
        }
        return methodLookup1;
    }

    public static Map<String,List<MethodReference>> listBySupertypeName(Map<String,Pair<String,MethodReference>> methodLookup1) {
        HashMap<String,List<MethodReference>> methodLookup2 = new HashMap<>();
        for (Map.Entry<String,Pair<String,MethodReference>> entry : methodLookup1.entrySet()) {
            Pair<String,MethodReference> refs = entry.getValue();
            String supertypeName = refs.getL();
            MethodReference methodLink = refs.getR();
            resolver.resolveLink(methodLink.getLink());
            List<MethodReference> inheritedMethods = methodLookup2.get(supertypeName);
            if (inheritedMethods == null) {
                List<MethodReference> newList = new ArrayList<>();
                newList.add(methodLink);
                methodLookup2.put(supertypeName, newList);
            } else {
                methodLookup2.get(supertypeName).add(methodLink);
            }
        }
        return methodLookup2;
    }

    public static void processInheritedMethods(TypeNode typeNode) {
        if (typeNode.getSupertypes().size() > 1) {
            Map<String, Pair<String, MethodReference>> overriddenMethods = gatherOverriddenMethods(typeNode);
            Map<String,List<MethodReference>> methodLookup2 = listBySupertypeName(overriddenMethods);
            // Copy methodLookup2 into typeNode's inheritedMethods hash table
            // but use a VariableType as the key
            for (Map.Entry<String,List<MethodReference>> entry : methodLookup2.entrySet()) {
                List<MethodReference> methods = entry.getValue();
                VariableType supertypeRef = VariableType.parse(entry.getKey());
                resolver.resolveVariableType(supertypeRef);
                typeNode.getInheritedMethods().put(supertypeRef, methods);
            }
        }
    }

    // This is related to TextAssembler#gatherOverriddenMethods and ElementModeller#setMethodAnnotations
    public static void linkBaseMethod(Link baseMethodLink, String baseTypeName) {
        if (baseMethodLink.getTarget().getName().isMember()) {
            String methodName = baseMethodLink.getTarget().getName().simpleName();
            Name typeName = new Name(baseTypeName, null);
            baseMethodLink.getTarget().setName(typeName);
            baseMethodLink.setKind(Link.Kind.UNRESOLVED);
            resolver.resolveLink(baseMethodLink);
            baseMethodLink.setAnchor(methodName.toLowerCase());
            baseMethodLink.setKind(Link.Kind.METHOD);
        }
    }

    public static void associateMethodWithType(MethodNode methodNode) {
        ctx.setTypeName(methodNode.getOwnerName().fullyQualifiedName());
        ctx.setMethodName(methodNode.getName().simpleName());
        TypeNode ownerType = api.getTypeNode(methodNode.getOwnerName());
        if (ownerType == null) {
            ctx.reportError("Unable to determine owner of method");
            return;
        }

        if (methodNode.isConstructor()) {
            methodNode.setName(ownerType.getName());
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
        ctx.setTypeName(typeNode.getName().fullyQualifiedName());
        List<VariableType> interfaces = typeNode.getImplementedInterfaces();
        for (VariableType interfaceRef : interfaces) {
            String interfaceName = interfaceRef.getRawTypeName().toString();
            InterfaceNode interfaceType = (InterfaceNode) api.getTypeNode(interfaceName);
            if (interfaceType == null) {
                interfaceType = getStandardInterface(interfaceRef);
            }
            if (interfaceType == null) {
                ctx.reportError("Can't find interface: " + interfaceName);
                return;
            }
            setImplementingClass(interfaceType, typeNode);
            setMethodsSpecifier(interfaceType, typeNode);
        }
    }

    public static InterfaceNode getStandardInterface(VariableType interfaceRef) {
        String qualifiedInterfaceName = interfaceRef.getRawTypeName().toString();
        Class<?> standardClass = JreUtils.loadClass(qualifiedInterfaceName);
        if (standardClass == null) {
            return null;
        }
        String packageName = standardClass.getPackageName();
        Method[] methods = standardClass.getMethods();
        Name interfaceName = new Name(qualifiedInterfaceName, standardClass.getPackageName());
        InterfaceNode interfaceNode = new InterfaceNode(interfaceName);
        for (Method method : methods) {
            Name methodName = new Name(method.getName(), qualifiedInterfaceName, packageName);
            MethodNode methodNode = new MethodNode("", methodName);
            interfaceNode.addMethod(methodNode);
        }
        return interfaceNode;
    }

    public static void setImplementingClass(InterfaceNode interfaceNode, TypeNode typeNode) {
        Reference toType = new Reference("", typeNode.getName());
        Reference fromPackage = new Reference("",new Name(null, interfaceNode.getPackageName()));
        Link implementingClassLink = Link.to(toType).from(fromPackage);
        resolver.resolveLink(implementingClassLink);
        interfaceNode.addImplementingClass(implementingClassLink);
    }

    public static void setMethodsSpecifier(InterfaceNode interfaceNode, TypeNode typeNode) {
        for (MethodNode methodNode : typeNode.getMethods()) {
            for (MethodNode interfaceMethod : interfaceNode.getMethods()) {
                if (interfaceMethod.simplifiedSignature().equals(methodNode.simplifiedSignature())) {
                    Reference toType = new Reference("", interfaceNode.getName());
                    Reference fromPackage = new Reference("", new Name(null, typeNode.getPackageName()));
                    Link specifiedByLink = Link
                            .to(toType).from(fromPackage);
                    resolver.resolveLink(specifiedByLink);
                    methodNode.setSpecifiedBy(specifiedByLink);
                    break;
                }
            }
        }
    }

    static void resolveLinksForParams(List<ParamNode> params) {
        for (ParamNode param : params) {
            resolver.resolveVariableType(param.getType());
            resolveLinksForReferences(param);
        }
    }

    public static void resolveLinksForReferences(Node node) {
        for (Link link : node.getReferences()) {
            if (link instanceof FileLink fileLink) {
                String docRoot = resolver.resolveRoot();
                String target = fileLink.getFileName();
                Path path = Path.of(docRoot, target);
                URI uri = URI.create(path.toString());
                fileLink.setUri(uri);
            } else {
                if (!resolver.resolveLink(link)) {
                    reportFailure(link);
                }
            }
        }
    }

    //
    //
    //

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

    public static void processJavadocComments(Api api) {
        for (Link link : api.getLinks()) {
            if (link.getOrigin() != null) {
                Name origin = link.getOrigin().getName();
                ctx.setPackageName(origin.packageName().toString());
                ctx.setTypeName(origin.typeName().toString());
                if (!resolver.resolveLink(link)) {
                    reportFailure(link);
                }
            }
        }
    }

    public static void addJavadocToRecords(Api api) {
        for (TypeNode recordView : api.getRecords()) {
            RecordNode recordNode = (RecordNode) recordView;
            for (MethodNode method : recordNode.getMethods()) {
                Text text = method.getFirstSentence();
                if (text.isEmpty()) {
                    switch(method.getName().simpleName()) {
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
                            text = Text.of(String.format("Returns the value of the `%s` record component.", method.getName().simpleName()));
                            break;
                    }
                    method.setFirstSentence(text);
                }
            }
        }
    }

    private static Reference here() {
        Name name = new Name(ctx.getTypeName(), ctx.getPackageName());
        return new Reference(ctx.getModuleName(), name);
    }
}
