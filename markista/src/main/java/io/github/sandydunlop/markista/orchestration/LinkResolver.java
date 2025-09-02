package io.github.sandydunlop.markista.orchestration;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.sandydunlop.markista.core.Configuration;
import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.Link;
import io.github.sandydunlop.markista.model.Link.Kind;
import io.github.sandydunlop.markista.model.Link.Scope;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.TypeNode;
import io.github.sandydunlop.markista.model.TypeReference;
import io.github.sandydunlop.markista.modelling.StandardModeller;

public class LinkResolver {
    private static final String DOT_HTML = ".html";
    private static final String JAVA_24_URL = "https://docs.oracle.com/en/java/javase/24/docs/api/";

    Api api;
    Context ctx;
    private StandardModeller modeller = new StandardModeller();
    private final List<String> primitives = Arrays.asList("boolean","byte","char","short","int","long","float","double");
    private Map<String,String> packagesToModules;
    private Map<String,Module> namedModules;

    private ModulePath siblingModulePath = null;

    public LinkResolver(Api a, Context c) {
        api = a;
        ctx = c;
        loadPackages();

        if (Configuration.getModulePaths() != null && !Configuration.getModulePaths().isEmpty()) {
            siblingModulePath = new ModulePath(c, Configuration.getModulePaths());
        }
    }

    /// Resolve links to types referenced by a [TypeReference].
    /// @param typeRef a TypeReference object describing the links
    public void resolveTypeRererence(TypeReference typeRef) {
        switch (typeRef) {
            case TypeReference.Generic generic -> {
                resolve(generic.getLink());
                resolveTypeRererence(generic.getParams());
            }
            case TypeReference.Sequence sequence -> {
                for (TypeReference element : sequence) {
                    resolveTypeRererence(element);
                }
            }
            default -> resolve(typeRef.getLink());
        }
    }

    public void resolveLink(Link link) {
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
            link.setAnchor(targetName.substring(pos + 1));
            String methodSignature = targetName.substring(pos + 1);
            link.setMethodSignature(methodSignature);
            pos = methodSignature.indexOf("(");
            if (pos > -1) {
                link.setMethodName(methodSignature.substring(0, pos));
            } else {
                link.setMethodName(methodSignature);
            }
        }

        pos = link.getTarget().indexOf("(");
        if (pos > -1) {
            link.setTarget(link.getTarget().substring(0, pos));
        }

        if (link.getLabel() == null || link.getLabel().isEmpty()) {
            link.setLabel(link.getTarget());
        }

        resolve(link);

        if (!link.getAnchor().isEmpty() && link.getKind() != Link.Kind.URL) {
            // Issue: https://github.com/sandydunlop/markista/issues/1
            // Workaround:
            // Remove the parentheses from after method names in anchor
            // links to Markdown pages for now. Anchors in the Markdown
            // are currently headings without parameters.
            link.setKind(Link.Kind.METHOD);
            link.setAnchor(removeParentheses(link.getAnchor()));
        }
    }

    /// Removes parentheses and what they contain from an expression
    /// @param expression An expression such as `classname.method(parameter)`.
    /// @return The expression with the parentheses removed
    public String removeParentheses(String expression) {
        int start = expression.indexOf('(');
        if (start > -1) {
            int end = expression.indexOf(')', start);
            String r = "";
            if (start > 0) {
                r = expression.substring(0, start);
            }
            if (end < expression.length()) {
                r += expression.substring(end + 1);
            }
            return r;
        }
        return expression;
    }

    public boolean resolve(Link link) {
        if (link == null || link.getTarget() == null || link.getTarget().isEmpty() || link.isResolved()) {
            return false;
        }

        link.setResolved(false);
        if (link.getOriginPackage().isEmpty() && ctx != null) {
            link.setOriginPackage(ctx.getPackageName());
        }
        if (link.getOriginType().isEmpty() && ctx != null) {
            link.setOriginType(ctx.getTypeName());
        }

        if (link.getTarget().contains("://")) {
            link.setUri(link.getTarget());
            link.setKind(Link.Kind.URL);
            link.setResolved(true);
            return true;
        }

        resolveUnsupported(link);
        if (link.getKind() == Link.Kind.UNSUPPORTED) {
            return true;
        }

        if (link.getTarget().endsWith("/")) {
            link.setKind(Link.Kind.MODULE);
        }

        if (resolvePrimitiveOrVoid(link)) return true;
        if (resolveLocalPackageOrType(link)) return true;
        if (resolveLocalModule(link)) return true;
        if (resolveJrePackageOrType(link)) return true;
        if (resolveJreModule(link)) return true;

        return link.isResolved();
    }

    /// Checks if the target is unsupported by LinkResolver.
    /// @param link The target to be resolved.
    /// @return A [Link] with `Reference.Kind.UNKNOWN` if unsupported, else `Reference.Kind.NONE`.
    Link resolveUnsupported(Link link) {
        if (link.getTarget().equals("?") || link.getTarget().contains("<")) {
            link.setScope(Scope.UNKNOWN);
            link.setKind(Kind.UNSUPPORTED);
        }
        return link;
    }

    /// Checks if the target is a primitive type or void.
    /// @param link The target to be resolved.
    /// @return True if the reference resolved to primitive or void, else false.
    boolean resolvePrimitiveOrVoid(Link link) {
        if (link.getKind() != Kind.UNKNOWN && link.getKind() != Kind.PRIMITIVE && link.getKind() != Kind.VOID) {
            return false;
        }
        if ("void".equals(link.getTarget()) || "Void".equals(link.getTarget())){
            link.setScope(Scope.STANDARD);
            link.setKind(Kind.VOID);
            link.setResolved(true);
            return true;
        }
        if (primitives.contains(link.getTarget())){
            link.setScope(Scope.STANDARD);
            link.setKind(Kind.PRIMITIVE);
            link.setResolved(true);
            return true;
        }
        return false;
    }

    boolean resolveJreModule(Link link) {
        if (link.getKind() != Kind.UNKNOWN && link.getKind() != Kind.MODULE) {
            return false;
        }
        if (namedModules.containsKey(moduleName(link))) {
            Scope scope = Scope.STANDARD;
            if (siblingModulePath != null && siblingModulePath.hasModule(link.getTarget())) {
                scope = Scope.SIBLING;
            }
            resolvedModule(link, scope);
            String url = JAVA_24_URL + link.getModuleName() + "/module-summary.html";
            link.setUri(url);
            link.setKind(Link.Kind.URL);
            return true;
        }
        return false;
    }

    boolean resolveLocalModule(Link link) {
        if (link.getKind() != Kind.UNKNOWN && link.getKind() != Kind.MODULE) {
            return false;
        }
        ModuleNode module = api.getModuleNode(moduleName(link));
        if (module != null) {
            resolvedModule(link, Link.Scope.LOCAL);
            String apiRoot = Relativizer.relativize(link.getOriginPackage(), "");
            Path path = Path.of(apiRoot, "..", link.getTarget());
            link.setUri(path.toString());
            return true;
        }
        return false;
    }

    private String moduleName(Link link) {
        String moduleName = link.getTarget();
        if (moduleName.endsWith("/")) {
            moduleName = moduleName.substring(0, moduleName.length() - 1);
        }
        return moduleName;
    }

    /// Resolves a standard Java package or type to an external documentation URL.
    /// @param link The link to be resolved
    /// @return True if the reference resolved to a standard package or type.
    boolean resolveJrePackageOrType(Link link) {
        if (link.getKind() != Kind.UNKNOWN && link.getKind() != Kind.PACKAGE && link.getKind() != Kind.TYPE) {
            return false;
        }
        Class<?> jreType = JreUtils.loadClass(link.getTarget());
        if (jreType != null) {
            Scope scope = Scope.STANDARD;
            if (siblingModulePath != null && siblingModulePath.hasClass(link.getTarget())) {
                scope = Scope.SIBLING;
            }
            resolvedType(link, modeller.modelType(jreType), scope);
            link.setModuleName(packagesToModules.get(link.getPackageName()));
            String packageName = link.getPackageName();
            String url = JAVA_24_URL + link.getModuleName();
            url += "/" + packageName.replace(".", "/");
            if (link.getNestedClassName().isEmpty()) {
                url += "/" + link.getSimpleClassName();
            } else {
                url += "/" + link.getNestedClassName();
            }
            url += DOT_HTML;
            link.setUri(url);
            link.setKind(Link.Kind.URL);
            return true;
        }
        String moduleName = packagesToModules.get(link.getTarget());
        if (moduleName != null) {
            resolvedPackage(link, Link.Scope.STANDARD);
            String packageName = link.getPackageName();
            link.setModuleName(packagesToModules.get(packageName));
            String url = JAVA_24_URL + link.getModuleName();
            url += "/" + packageName.replace(".", "/");
            url += DOT_HTML;
            link.setUri(url);
            link.setKind(Link.Kind.URL);
            return true;
        }
        return false;
    }

    boolean resolveLocalPackageOrType(Link link) {
        if (link.getKind() != Kind.UNKNOWN && link.getKind() != Kind.PACKAGE && link.getKind() != Kind.TYPE) {
            return false;
        }
        TypeNode typeNode = api.getTypeNode(link.getTarget());
        if (typeNode == null) {
            for (TypeNode type : api.getTypes()) {
                String qualifiedName = type.getQualifiedName();
                if (qualifiedName.endsWith("." + link.getTarget())) {
                    typeNode = type;
                    break;
                }
            }
        }
        if (typeNode != null) {
            resolvedType(link, typeNode, Link.Scope.LOCAL);
            String uri = relativizeWithModules(link.getOriginPackage(), link.getPackageName());
            if (!uri.isEmpty()) {
                uri += "/";
            }
            uri += typeNode.getSimpleName();
            link.setUri(uri);
            return true;
        }
        PackageNode packageNode = api.getPackageNode(link.getTarget());
        if (packageNode != null) {
            resolvedPackage(link, Link.Scope.LOCAL);
            link.setUri(relativizeWithModules(link.getOriginPackage(), link.getPackageName()));
            return true;
        }
        return false;
    }




    private void resolvedModule(Link link, Link.Scope scope) {
        link.setKind(Link.Kind.MODULE);
        link.setScope(scope);
        link.setModuleName(moduleName(link));
        link.setResolved(true);
    }

    private void resolvedPackage(Link link, Link.Scope scope) {
        link.setKind(Link.Kind.PACKAGE);
        link.setScope(scope);
        link.setPackageName(link.getTarget());
        link.setResolved(true);
    }

    private void resolvedType(Link link, TypeNode type, Link.Scope scope) {
        link.setKind(Link.Kind.TYPE);
        link.setScope(scope);
        link.setPackageName(type.getPackageName());
        link.setClassName(type.getName());
        link.setQualifiedClassName(type.getQualifiedName());
        link.setSimpleClassName(type.getSimpleName());
        int nestedLength = type.getQualifiedName().length() - type.getPackageName().length();
        if (nestedLength > type.getSimpleName().length()) {
            link.setNestedClassName(type.getQualifiedName()
                    .substring(type.getQualifiedName().length() - nestedLength + 1));
        }
        link.setResolved(true);
    }




    /// Produces a relative path considering modules between two packages.
    /// If packages belong to different modules, the relative path includes module directories.
    /// @param from The package name for the source.
    /// @param to The package name for the target.
    /// @return The relative path string.
    String relativizeWithModules(String from, String to) {
        PackageNode toPackage = api.getPackageNode(to);
        if (toPackage == null) {
            ctx.reportError("Error resolving package for: " + to);
            return null;
        }
        String fromModuleName = "";
        String toModuleName = "";
        ModuleNode fromModule = api.getModuleNode(ctx.getModuleName());
        ModuleNode toModule = api.getModuleNode(toPackage.getModuleName());
        if (fromModule != null) {
            fromModuleName = fromModule.getName();
        }
        if (toModule != null) {
            toModuleName = toModule.getName();
        }
        if (fromModuleName.equals(toModuleName)) {
            // to and from are members of the same module
            return Relativizer.relativize(from, to);
        } else {
            return Path.of(Relativizer.relativize(from, ""), toModuleName, Relativizer.relativize("", to)).toString();
        }
    }

    /// Computes a relative path from the current package context, considering sibling modules.
    /// @param from The source package name.
    /// @param to The target package name.
    /// @return The relative path string including sibling module base if applicable.
    static String relativizeWithSiblingModule(String from, String to, String toModule) {
        String toRoot = Relativizer.relativize(from, "");
        String toTarget = Relativizer.relativize("", to);
        Path path = Path.of(toRoot, "..", toModule, toTarget);
        return path.toString();
    }

    public void loadPackages() {
        packagesToModules = new HashMap<>();
        namedModules = new HashMap<>();
        Set<Module> modules = ModuleLayer.boot().modules();
        for (Module module : modules) {
            namedModules.put(module.getName(), module);
            for (String packageName : module.getPackages()) {
                packagesToModules.put(packageName, module.getName());
            }
        }
    }
}
