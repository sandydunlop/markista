package io.github.sandydunlop.markista.orchestration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.sandydunlop.markista.core.Configuration;
import io.github.sandydunlop.markista.core.Context;

import io.github.sandydunlop.cascara.model.Api;
import io.github.sandydunlop.cascara.model.ExternalLink;
import io.github.sandydunlop.cascara.model.Link;
import io.github.sandydunlop.cascara.model.Link.Kind;
import io.github.sandydunlop.cascara.model.Link.Scope;
import io.github.sandydunlop.cascara.model.ModuleNode;
import io.github.sandydunlop.cascara.model.Name;
import io.github.sandydunlop.cascara.model.PackageNode;
import io.github.sandydunlop.cascara.model.Pair;
import io.github.sandydunlop.cascara.model.Reference;
import io.github.sandydunlop.cascara.model.TypeNode;
import io.github.sandydunlop.cascara.model.VariableType;
import io.github.sandydunlop.cascara.jreutil.JreUtil;
import io.github.sandydunlop.cascara.model.ModelUtil;

public class LinkResolver {
    private static final String DOT_HTML = ".html";
    private static final String JAVA_24_URL = "https://docs.oracle.com/en/java/javase/24/docs/api/";

    Api api;
    Context ctx;
    private final List<String> primitives = Arrays.asList("boolean","byte","char","short","int","long","float","double");
    private Map<String,String> jrePackagesToModules;
    private Map<String,Module> jreNamedModules;

    private ModulePath siblingModulePath = null;

    List<ExternalLink> externalLinks = new ArrayList<>();
    String flattenedDirectories = "";

    public LinkResolver(Api a, Context c) {
        api = a;
        ctx = c;
        loadPackages();
        loadExternalLinks();
        flattenedDirectories = ctx.getFlattenedDirectories();
        if (Configuration.getModulePaths() != null && !Configuration.getModulePaths().isEmpty()) {
            siblingModulePath = new ModulePath(c, Configuration.getModulePaths());
        }
    }

    public String resolveRoot() {
        Name here = ModelUtil.createName(null, ctx.getPackageName());
        URI uri = relativize(flattenDirectory(here), null, null);
        return uri.toString();
    }

    /// Resolve links to types referenced by a [VariableType].
    /// @param typeRef a VariableType object describing the links
    public void resolveVariableType(VariableType typeRef) {
        switch (typeRef) {
            case VariableType.Generic generic -> {
                resolveLink(generic.getLink());
                resolveVariableType(generic.getParams());
            }
            case VariableType.Sequence sequence -> {
                for (VariableType element : sequence) {
                    resolveVariableType(element);
                }
            }
            default -> resolveLink(typeRef.getLink());
        }
    }

    public boolean resolveLink(Link link) {
        if (link == null) return false;
        if (link.isResolved()) return true;
        if (link.getTarget() == null) {
            if (link.getUri() != null) {
                link.setKind(Link.Kind.WEB);
                link.setResolved(true);
                return true;
            } else {
                ctx.reportWarning("No link target supplied");
                return false;
            }
        }

        if (link.getOrigin() == null) {
            Name originName = ModelUtil.createName(ctx.getTypeName(), ctx.getPackageName());
            Reference origin = new Reference(ctx.getModuleName(), originName);
            link.setOrigin(origin);
        }

        qualify(link.getOrigin());
        qualify(link.getTarget());

        boolean success;
        link.setResolved(false);

        if (link.getTarget().getName() != null && link.getTarget().getName().isMember()) {
            Name targetName = link.getTarget().getName();
            Name typeName = targetName.firstComponents(-1);
            String methodName = targetName.simpleName();
            link.getTarget().setName(typeName);
            link.setMethodName(methodName);
            link.setAnchor(methodName.toLowerCase());
            success = resolve(link);
            if (success) {
                link.setKind(Link.Kind.METHOD);
            }
            link.getTarget().setName(targetName);
        } else {
            success = resolve(link);
        }

        return success;
    }

    /// Checks if the target is a primitive type or void.
    /// @param link The target to be resolved.
    /// @return True if the reference resolved to primitive or void, else false.
    boolean resolvePrimitiveOrVoid(Link link) {
        if (link.getTarget() == null) {
            return false;
        }
        String target = link.getTarget().getName().fullyQualifiedName();
        if ("void".equals(target) || "Void".equals(target)){
            link.setScope(Scope.STANDARD);
            link.setKind(Kind.VOID);
            link.setResolved(true);
            return true;
        }
        if (primitives.contains(target)){
            link.setScope(Scope.STANDARD);
            link.setKind(Kind.PRIMITIVE);
            link.setResolved(true);
            return true;
        }
        return false;
    }



    private void resolvedModule(Link link, Link.Scope scope) {
        link.setKind(Link.Kind.MODULE);
        link.setScope(scope);
        link.setResolved(true);
    }

    private void resolvedPackage(Link link, Link.Scope scope) {
        link.setKind(Link.Kind.PACKAGE);
        link.setScope(scope);
        link.setResolved(true);
    }

    private void resolvedType(Link link, Link.Scope scope) {
        link.setKind(Link.Kind.TYPE);
        link.setScope(scope);
        link.setResolved(true);
    }

    private void resolvedExternal(Link link, ExternalLink extLink, String packageName, String typeName) {
        if (typeName.isEmpty()) {
            link.setKind(Link.Kind.PACKAGE);
        } else {
            link.setKind(Link.Kind.TYPE);
        }
        link.setScope(extLink.isWebLink() ? Link.Scope.EXTERNAL : Link.Scope.SIBLING);

        String up = Relativizer.relativize(ctx.getPackageName(), "");
        Relativizer.setFlattenedDirectories(extLink.getApi().commonBase());
        String down = Relativizer.relativize("", packageName);
        Relativizer.setFlattenedDirectories(api.commonBase());

        if (extLink.isWebLink()) {
            String moduleName = extLink.getPackageToModule().get(packageName);
            URI uri = extLink.getUri();
            uri = uri.resolve(moduleName);
            uri = uri.resolve(packageName.replace(".", "/"));
            if (!typeName.isEmpty()) {
                uri = uri.resolve(typeName);
            }
            link.setUri(uri);
            link.setResolved(true);
        } else {
            String directory = extLink.getUri().getPath().toString();
            String path = Path.of(up, directory, down).normalize().toString();
            if (typeName.isEmpty()) {
                link.setUri(URI.create(path));
            } else {
                link.setUri(URI.create(path + "/" + typeName));
            }
        }

        link.setResolved(true);
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

    void loadPackages() {
        jrePackagesToModules = new HashMap<>();
        jreNamedModules = new HashMap<>();
        Set<Module> modules = ModuleLayer.boot().modules();
        for (Module module : modules) {
            jreNamedModules.put(module.getName(), module);
            for (String packageName : module.getPackages()) {
                jrePackagesToModules.put(packageName, module.getName());
            }
        }
    }

    void loadExternalLinks() {
        externalLinks = new ArrayList<>();
        for (String path : Configuration.getLinks()) {
            ExternalLink extLink = new ExternalLink(URI.create(path), ctx.getOutputDirectory());
            try {
                extLink.load();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            externalLinks.add(extLink);
        }
    }

    public boolean resolve(Link link) {
        if (link.getTarget().getName() != null) {
            return resolvePackageOrType(link);
        } else {
            return resolveModule(link);
        }
    }

    private boolean resolvePackageOrType(Link link) {
        if (resolvePrimitiveOrVoid(link)) return true;
        if (tryResolveLocalType(link)) return true;
        if (tryResolveLocalPackage(link)) return true;
        if (tryResolveJreType(link)) return true;
        if (tryResolveJrePackage(link)) return true;
        return tryResolveExternalPackageOrType(link);
    }

    private boolean resolveModule(Link link) {
        if (tryResolveLocalModule(link)) return true;
        if (tryResolveJreModule(link)) return true;
        return tryResolveExternalModule(link);
    }

    boolean tryResolveJreModule(Link link) {
        Reference target = link.getTarget();
        if (target.getModuleName().isEmpty()) {
            return false;
        }
        if (jreNamedModules.containsKey(target.getModuleName())) {
            Scope scope = Scope.STANDARD;
            if (siblingModulePath != null && siblingModulePath.hasModule(target.getModuleName())) {
                scope = Scope.SIBLING;
            }
            resolvedModule(link, scope);
            String url = JAVA_24_URL + target.getModuleName() + "/module-summary.html";
            link.setUri(URI.create(url));
            return true;
        }
        return false;
    }

    boolean tryResolveLocalModule(Link link) {
        Reference target = link.getTarget();
        if (target.getModuleName().isEmpty()) {
            return false;
        }
        ModuleNode module = api.getModuleNode(target.getModuleName());
        if (module != null) {
            resolvedModule(link, Link.Scope.LOCAL);
            link.setUri(relativize(link.getOrigin().getName(), null, target.getModuleName()));
            return true;
        }
        return false;
    }

    boolean tryResolveExternalModule(Link link) {
        Reference target = link.getTarget();
        if (target.getModuleName().isEmpty()) {
            return false;
        }
        for (ExternalLink extLink : externalLinks) {
            for (ModuleNode moduleNode : extLink.getApi().getModules()) {
                if (moduleNode.getName().equals(target.getModuleName())) {
                    resolvedModule(link, extLink.isWebLink() ? Scope.EXTERNAL : Scope.SIBLING);
                    link.setUri(relativize(link.getOrigin().getName(), null, target.getModuleName()));
                    return true;
                }
            }
        }
        return false;
    }

    boolean tryResolveJreType(Link link) {
        Reference target = link.getTarget();
        String qualifiedBinaryName = target.getName().fullyQualifiedBinaryName();
        Class<?> jreType = JreUtil.loadClass(qualifiedBinaryName);
        if (jreType != null) {
            Scope scope = Scope.STANDARD;
            String typeName = link.getTarget().getName().fullyQualifiedName();
            if (siblingModulePath != null && siblingModulePath.hasClass(typeName)) {
                scope = Scope.SIBLING;
            }
            resolvedType(link, scope);
            String packageName = jreType.getPackageName();
            String moduleName = jrePackagesToModules.get(packageName);
            String className = jreType.getSimpleName();

            String url = JAVA_24_URL + moduleName;
            url += "/" + packageName.replace(".", "/");
            url += "/" + className;
            url += DOT_HTML;
            link.setUri(URI.create(url));
            return true;
        }
        return false;
    }

    boolean tryResolveJrePackage(Link link) {
        Reference target = link.getTarget();
        String moduleName = jrePackagesToModules.get(target.getName().fullyQualifiedName());
        if (moduleName != null) {
            resolvedPackage(link, Link.Scope.STANDARD);
            String packageName = target.getName().fullyQualifiedName();
            String url = JAVA_24_URL + moduleName;
            url += "/" + packageName.replace(".", "/");
            url += DOT_HTML;
            link.setUri(URI.create(url));
            return true;
        }
        return false;
    }

    boolean tryResolveLocalType(Link link) {
        Reference origin = link.getOrigin();
        Reference target = link.getTarget();
        TypeNode typeNode = api.getTypeNode(target.getName());
        if (typeNode != null) {
            Name typeName = ModelUtil.createName(typeNode.getName().fullyQualifiedName(), typeNode.getPackageName());
            String module = "";
            Name originName = flattenDirectory(origin.getName().packageName());
            Name targetName = flattenDirectory(typeName);
            link.setUri(relativize(originName, targetName, module));
            resolvedType(link, Link.Scope.LOCAL);
            return true;
        }
        return false;
    }

    boolean tryResolveLocalPackage(Link link) {
        Reference origin = link.getOrigin();
        Reference target = link.getTarget();
        PackageNode packageNode = api.getPackageNode(target.getName());
        if (packageNode != null) {
            String module = "";
            Name originName = flattenDirectory(origin.getName().packageName());
            Name targetName = flattenDirectory(target.getName());
            link.setUri(relativize(originName, targetName, module));
            resolvedPackage(link, Link.Scope.LOCAL);
            return true;
        }
        return false;
    }

    boolean tryResolveExternalPackageOrType(Link link) {
        Reference target = link.getTarget();
        Name targetName = target.getName();
        for (int i=targetName.componentCount(); i>0; i--) {
            Name candidateName = targetName.firstComponents(i);
            String candidate = candidateName.toString();
            for (ExternalLink extLink : externalLinks) {
                if (extLink.getPackages().contains(candidate)) {
                    String packageName = candidate;
                    String typeName = candidateName.lastComponents(-i).toString();
                    resolvedExternal(link, extLink, packageName, typeName);
                    return true;
                }
            }
        }
        return false;
    }

    URI relativize(Name from, Name to, String module) {
        // a.b.C.D
        URI uri;
        int commonCount = 0;
        try {
            if (to == null) {
                uri = new URI("../".repeat(from.componentCount()));
            } else {
                commonCount = from.commonComponentCount(to);
                uri = new URI("../".repeat(from.componentCount() - commonCount));
            }
            if (module != null && !module.isEmpty()) {
                uri = uri.resolve("../" + module + "/");
            }
            if (to != null) {
                Name toPackageName = to.packageName().lastComponents(-commonCount);
                Name toTypeName = to.typeName();
                if (!toPackageName.isEmpty()) {
                    String toPackage = toPackageName.toString().replace(".", "/");
                    uri = uri.resolve(toPackage + "/");
                }
                if (!toTypeName.isEmpty()) {
                    String toType = to.typeName().toString();
                    uri = uri.resolve(toType);
                    String tmp = uri.toString();
                    if (tmp.endsWith("/")) {
                        tmp = tmp.substring(0, tmp.length() - 1);
                        uri = new URI(tmp);
                    }
                }
            }
            return uri;
        } catch (URISyntaxException _) {
            // Ignore it
        }
        return null;
    }




    /// Removes prefix directories from a path if flattenedDirectories is set and matches.
    /// @param path The package name or path to flatten.
    /// @return The adjusted path or original if no flattening applies.
    Name flattenDirectory(Name path) {
        if (path.isEmpty()) {
            return path;
        }
        if (flattenedDirectories != null && !flattenedDirectories.isEmpty()) {
            int flattenedComponents = 1;
            for (int i = 0; i < flattenedDirectories.length(); i++) {
                if (flattenedDirectories.charAt(i) == '.') {
                    flattenedComponents++;
                }
            }
            return path.lastComponents(-flattenedComponents);
        }
        return path;
    }

    boolean qualify(Reference ref) {
        Name name = ref.getName();
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (qualifyLocalReference(ref)) return true;
        if (qualifyJreReference(ref)) return true;
        return qualifyExternalReference(ref);
    }

    private boolean qualifyLocalReference(Reference ref) {
        Name name = ref.getName();
        for (int i = name.componentCount(); i>0; i--) {
            Name candidate = name.firstComponents(i);
            PackageNode packageNode = api.getPackageNode(candidate);
            if (packageNode != null) {
                name.setPackageComponentCount(i);
                if (i < name.componentCount()) {
                    // Part of `name` is a package name
                    if (api.getTypeNode(name) == null) {
                        if (api.getTypeNode(name.firstComponents(-1)) != null) {
                            // It is a member
                            name.setIsMember(true);
                            ref.setModule(packageNode.getModuleName());
                            return true;
                        } else {
                            // It wasn't found
                        }
                    } else {
                        // It is a type name
                        name.setIsType(true);
                        ref.setModule(packageNode.getModuleName());
                        return true;
                    }
                } else {
                    // It is a package name
                    name.setIsPackage(true);
                    ref.setModule(packageNode.getModuleName());
                    return true;
                }
            }
        }
        // Try matching on part of the name
        String nameString = name.toString();
        for (TypeNode typeNode : api.getTypes()) {
            String simpleTypeName = typeNode.getName().simpleName();
            if (simpleTypeName.equals(nameString)) {
                ref.setName(typeNode.getName());
                return true;
            }
        }
        return false;
    }

    private boolean qualifyJreReference(Reference ref) {
        Name name = ref.getName();
        Class<?> jreType = JreUtil.loadClass(name.fullyQualifiedName());
        if (jreType == null) {
            // Check if the last component of the name is a member
            Name candidate = name.firstComponents(-1);
            jreType = JreUtil.loadClass(candidate.fullyQualifiedName());
            if (jreType != null) {
                name.setIsMember(true);
            }
        }
        if (jreType!= null) {
            Name packageName = ModelUtil.createName(jreType.getPackageName());
            name.setPackageComponentCount(packageName.componentCount());
            if (name.componentCount() == packageName.componentCount()) {
                name.setIsPackage(true);
            } else {
                if (!name.isMember()) {
                    name.setIsType(true);
                }
            }
            ref.setModule(jreType.getModule().getName());
            return true;
        }
        return false;
    }

    private boolean qualifyExternalReference(Reference ref) {
        Name name = ref.getName();
        Pair<String,String> moduleAndPackage = findExternalPackage(name);
        if (moduleAndPackage != null) {
            String module = moduleAndPackage.getL();
            String pkg = moduleAndPackage.getR();
            Name packageName = ModelUtil.createName(pkg);
            name.setPackageComponentCount(packageName.componentCount());
            ref.setModule(module);
            return true;
        }
        return false;
    }

    private Pair<String,String> findExternalPackage(Name targetName) {
        for (int i=targetName.componentCount(); i>0; i--) {
            Name candidateName = targetName.firstComponents(i);
            String candidate = candidateName.toString();
            for (ExternalLink extLink : externalLinks) {
                if (extLink.getPackages().contains(candidate)) {
                    String module = extLink.getPackageToModule().get(candidate);
                    return Pair.of(module, candidate);
                }
            }
        }
        return null;
    }
}

