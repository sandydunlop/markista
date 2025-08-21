package io.github.sandydunlop.markista.util;

import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.DirectiveNode;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.Node;
import io.github.sandydunlop.markista.model.Pair;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.RecordTypeNode;
import io.github.sandydunlop.markista.model.Reference;
import io.github.sandydunlop.markista.model.Text;
import io.github.sandydunlop.markista.model.Text.SegmentKind;
import io.github.sandydunlop.markista.model.TypeNode;
import io.github.sandydunlop.markista.model.TypeView;

import java.nio.file.Path;
import java.util.List;

public class TextAssembler {
    private static Context ctx;
    private static Api api;

    private TextAssembler() {
        // Nothing to see here
    }

    /// Generates [Text] objects for links to types and links in Javadoc text.
    /// This is where we decide if the label for those links shows qualified names or simplified names.
    /// @param a The API model
    /// @param context The doclet context to keep track of why package and type are being processed
    public static void assembleTextAndLinks(Api a, Context context) {
        api = a;
        ctx = context;

        processModules(api);

        // Links from types used in methods and fields
        for (TypeView typeView : api.getTypes()) {
            processTypeNode((TypeNode) typeView);

        }
        processJavadocComments(api);
        addJavadocToRecords(api);
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

    static void processTypeNode(TypeNode typeNode) {
        ctx.setPackageName(typeNode.getPackageName());
        ctx.setTypeName(typeNode.getQualifiedName());

        // Types
        TypeNode ownerTypeNode = api.getTypeNode(typeNode.getOwner());
        if (ownerTypeNode != null) {
            Reference ref = Reference.to(ownerTypeNode.getQualifiedName())
                    .from(ctx.getPackageName())
                    .withKind(Reference.Kind.TYPE)
                    .withLabel(ownerTypeNode.getQualifiedName());
            LinkResolver.resolve(ref);
            typeNode.setEnclosingClassRef(ref);
        }
        for (Reference implementedInterfaceRef : typeNode.getImplementedInterfaces()) {
            LinkResolver.resolve(implementedInterfaceRef);
        }
        for (Pair<Reference,Text> pair : typeNode.getSupertypes()) {
            Reference supertypeReference = pair.getL();
            pair.getR().append(link(supertypeReference, true));
        }
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

    static void processMethod(MethodNode method) {
        Reference returnTypeReference = Reference.to(method.getReturnTypeName()).from(ctx.getPackageName());
        method.setReturnTypeText(link(returnTypeReference, false));
        generateLinkTextsForParams(method.getParams()
                .stream()
                .filter(ParamNode.class::isInstance)
                .map(ParamNode.class::cast)
                .toList());
        if (method.getSpecifiedBy() != null && !method.getSpecifiedBy().getTarget().isEmpty()) {
            LinkResolver.resolve(method.getSpecifiedBy());
        }
        for (Reference thrownRef : method.getThrownTypes()) {
            LinkResolver.resolve(thrownRef);
        }
        Pair<Reference, Text> baseMethodPair = method.getBaseMethod();
        if (baseMethodPair != null) {
            String baseTypeName = baseTypeName(method);
            Reference baseMethodRef = baseMethodPair.getL();
            if (baseMethodRef.getTarget().isEmpty() && baseTypeName != null) {
                baseMethodRef.setTarget(baseTypeName + "#" + baseMethodRef.getMethodSignature());
            }
            Text linkText = link(baseMethodRef, false);

            baseMethodPair.setR(linkText);

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

    static Text processInheritDocTags(Text baseMethodText, Text text) {
        int segmentCount = text.getSegments().size();
        for (int i = segmentCount - 1; i > 0; i--) {
            Text.Segment segment = text.getSegments().get(i);
            if (segment.getKind() == SegmentKind.INHERIT) {
                Text before = text.subtext(0, i - 1);
                Text after = text.subtext(i + 1);
                Text inherited = baseMethodText;
                text = before.append(inherited).append(after);
            }
        }
        return text;
    }

    static String baseTypeName(MethodNode method){
        String methodSignature = method.signature();
        TypeNode type = api.getTypeNode(method.getOwnerName());

        if (type == null) {
            return null;
        }
        for (int i = type.getSupertypes().size() - 1; i > 0; i--) {
            Pair<Reference, Text> supertypePair = type.getSupertypes().get(i);
            TypeNode supertype = api.getTypeNode(supertypePair.getL().getTarget());
            if (supertype != null) {
                for (MethodNode inheritedMethod : supertype.getMethods()) {
                    if (inheritedMethod.signature().equals(methodSignature)) {
                        // Found it
                        return supertype.getQualifiedName();
                    }
                }
            } else {
                ctx.reportError("ERR");
            }
        } 
        return null;
    }

    static void processModules(Api api) {
        for (ModuleNode module : api.getModules()) {
            ctx.setModuleName(module.getName());
            // Constant field values
            for (FieldNode constant : module.getConstantValues()) {
                Reference ref = Reference.to(constant.getTypeName())
                        .from("")
                        .withLabel(constant.getTypeName());
                LinkResolver.resolve(ref);
                constant.setConstantValueReference(ref);
            }

            // Directives
            for (DirectiveNode directive : module.getDirectives()) {
                LinkResolver.resolve(directive.getReference());
                LinkResolver.resolve(directive.getInterface());
                for (Reference implementation : directive.getImplementations()) {
                    LinkResolver.resolve(implementation);
                }
                for (Reference pkg : directive.getPackages()) {
                    LinkResolver.resolve(pkg);
                }
            }
        }
    }

    static void processJavadocComments(Api api) {
        for (Reference link : api.getLinks()) {
            ctx.setPackageName(link.getOrigin());
            LinkResolver.resolve(link);
            if (link.getLabel().contains(".")) {
                link.setLabel(Utils.simplifyNames(link.getLabel()));
            }
        }
    }

    static void generateLinkTextsForParams(List<ParamNode> params) {
        for (ParamNode param : params) {
            Reference reference = Reference.to(param.getTypeName()).from(ctx.getPackageName());
            param.setTypeText(link(reference, false));
            generateLinkTextsForReferences(param);
        }
    }

    private static void generateLinkTextsForReferences(Node node) {
        for (Reference reference : node.getReferences()) {
            if (reference.getKind() == Reference.Kind.PAGE) {
                String relativePath = LinkResolver.relativize("");
                reference.setUri(Path.of(relativePath, reference.getTarget()).toString());
            } else {
                LinkResolver.resolve(reference);
            }
        }
    }

    /// Create a markdown link, automatically deciding what kind of link to make.
    /// @param reference a Reference object describing the link
    /// @param useQualifiedName If true, qualified names will be used in the link label
    /// @return markdown formatted link
    public static Text link(Reference reference, boolean useQualifiedName) {
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
                    .setKind(Text.SegmentKind.LINK)
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
            return linkGenerics(targetName, useQualifiedName);
        } else if (targetName.indexOf(',') > -1) {
            return splitAndLink(targetName, useQualifiedName);
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
        if (!anchor.isEmpty() && reference.getKind() != Reference.Kind.URL) {
            isLocalMethod = true;
            // Issue: https://github.com/sandydunlop/markista/issues/1
            // Workaround:
            // Remove the parentheses from after method names in anchor 
            // links to Markdown pages for now. Anchors in the Markdown
            // are currently headings without parameters.
            reference.setAnchor(Utils.removeParentheses(reference.getAnchor()));
        }
        setDisplayName(reference, displayName, isLocalMethod, useQualifiedName);

        Text.Segment link = Text.Segment.empty()
                .setKind(Text.SegmentKind.LINK)
                .setLink(reference)
                .setText(reference.getLabel());
        Text r = Text.empty();
        r.append(pre);
        r.append(link);
        r.append(post);
        return r;
    }

    private static void setDisplayName(Reference link, String displayName, boolean isLocalMethod, boolean useQualifiedName) {
        if (link.getLabel() == null) {
             link.setLabel(link.getTarget());
        }
        if (isLocalMethod) {
            link.setKind(Reference.Kind.METHOD);
            String methodName = link.getAnchor().substring(1);
            if (!link.getClassName().equals(ctx.getTypeName())) {
                link.setLabel(link.getLabel() + "." + methodName);
            } else {
                link.setLabel(methodName);
            }
        }
        if (displayName != null && !displayName.isEmpty()) {
            link.setLabel(displayName.replace("#","."));
        } else if (!useQualifiedName && canBeSimplified(link)) {
            link.setLabel(Utils.simplifyNames(link.getLabel()));
        }
        link.setLabel(escape(link.getLabel()));
    }

    private static boolean canBeSimplified(Reference link) {
        boolean originIsInPackage = !ctx.getPackageName().isEmpty();
        boolean kindCanBeSimplified = link.getKind() == Reference.Kind.TYPE || link.getKind() == Reference.Kind.METHOD || link.getKind() == Reference.Kind.URL;
        return kindCanBeSimplified && originIsInPackage;
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
    /// @param useQualifiedName If true, qualified type names will be displayed
    /// @return    A Text object with the qualified names changed to unqualified
    ///            names and links to types added
    public static Text linkGenerics(String str, boolean useQualifiedName) {
        if (str == null || str.isEmpty()) return Text.of(str);
        int openingChevron = str.indexOf("<");
        int closingChevron = str.lastIndexOf(">");
        String before = str.substring(0, openingChevron);
        String mid = str.substring(openingChevron + 1, closingChevron);
        String after = str.substring(closingChevron + 1);
        Text typeLink = link(Reference.to(before), useQualifiedName);

        Text midLinks;
        if (mid.contains("<")) {
            midLinks = linkGenerics(mid, useQualifiedName);
        } else {
            midLinks = splitAndLink(mid, useQualifiedName);
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
    /// @param useQualifiedName if true, the qualified version of the identifier is shown
    /// @return a list of links to types formatted as Markdown
    public static Text splitAndLink(String typesString, boolean useQualifiedName) {
        Text text = Text.empty();
        String[] types = typesString.split(",");
        for (String t : types) {
            String typeName = t.strip();
            if (!text.isEmpty()) {
                text.append(", ");
            }
            text.append(link(Reference.to(typeName), useQualifiedName));
        }
        return text;
    }
}
