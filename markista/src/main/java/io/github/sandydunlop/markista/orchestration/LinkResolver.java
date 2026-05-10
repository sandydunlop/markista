package io.github.sandydunlop.markista.orchestration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.qishr.cascara.lang.java.model.SemanticModel;
import io.github.qishr.cascara.lang.java.model.Link;
import io.github.qishr.cascara.lang.java.model.Link.Kind;
import io.github.qishr.cascara.lang.java.model.Link.Scope;
import io.github.qishr.cascara.lang.java.model.MethodNode;
import io.github.qishr.cascara.lang.java.model.ModuleNode;
import io.github.qishr.cascara.lang.java.model.NameUtil;
import io.github.qishr.cascara.lang.java.model.JlsName;
import io.github.qishr.cascara.lang.java.model.PackageNode;
import io.github.qishr.cascara.common.util.Pair;
import io.github.qishr.cascara.lang.java.model.Reference;
import io.github.qishr.cascara.lang.java.model.TypeNode;
import io.github.qishr.cascara.lang.java.model.VariableTypeNode;
import io.github.qishr.cascara.lang.java.util.JreUtil;
import io.github.sandydunlop.markista.core.Configuration;
import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.core.ExternalLink;

public class LinkResolver {
    private static final String DOT_HTML = ".html";
    private static final String JAVA_24_URL = "https://docs.oracle.com/en/java/javase/24/docs/api/";

    SemanticModel api;
    Context ctx;
    private final List<String> primitives = Arrays.asList("boolean","byte","char","short","int","long","float","double");
    private Map<String,String> jrePackagesToModules;
    private Map<String,Module> jreNamedModules;

    private ModulePath siblingModulePath = null;

    List<ExternalLink> externalLinks = new ArrayList<>();
    // String flattenedDirectories = "";

    public LinkResolver(SemanticModel a, Context c) {
        api = a;
        ctx = c;
        loadPackages();
        loadExternalLinks();
        // flattenedDirectories = ctx.getFlattenedDirectories();
        if (Configuration.getModulePaths() != null && !Configuration.getModulePaths().isEmpty()) {
            siblingModulePath = new ModulePath(c, Configuration.getModulePaths());
        }
    }

    public String resolveRoot() {
        JlsName here = NameUtil.createPackageName(ctx.getPackageName());
        URI uri = relativize(flattenDirectory(here, ctx.getCommonBasePath()), null, null);
        return uri.toString();
    }

    /// Resolve links to types referenced by a [VariableTypeNode].
    /// @param typeRef a VariableTypeNode object describing the links
    public void resolveVariableTypeNode(VariableTypeNode typeRef) {
        switch (typeRef) {
            case VariableTypeNode.Generic generic -> {
                resolveLink(generic.getLink());
                resolveVariableTypeNode(generic.getParams());
            }
            case VariableTypeNode.Sequence sequence -> {
                for (VariableTypeNode element : sequence) {
                    resolveVariableTypeNode(element);
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


        if (link.getTarget().getName() != null) {
            if (isVoid(link.getTarget().getName())) {
                link.setKind(Link.Kind.VOID);
                link.setResolved(true);
                return true;
            }
            if (isPrimitive(link.getTarget().getName())) {
                link.setKind(Link.Kind.PRIMITIVE);
                link.setResolved(true);
                return true;
            }
        }

        if (link.getOrigin() == null) {
            if (ctx.getPackageName() == null || ctx.getPackageName().isEmpty()) {
                if (ctx.getModuleName() == null || ctx.getModuleName().isEmpty()) {
                    // We're not in a package or a module.
                    // This is likely invalid. IllegalStateException?
                    return false;
                }
                // We're in a module, but not in a package
                Reference origin = NameUtil.createReference(ctx.getModuleName(), null);
                link.setOrigin(origin);
            } else {
                // We're in a package
                JlsName pkgName = NameUtil.createPackageName(ctx.getPackageName());
                JlsName originName = NameUtil.createTypeName(pkgName, ctx.getTypeName());
                Reference origin = NameUtil.createReference(ctx.getModuleName(), originName);
                link.setOrigin(origin);
            }
        }

        qualify(link.getOrigin());
        qualify(link.getTarget());

        boolean success;
        link.setResolved(false);

        if (link.getTarget().getName() != null && link.getTarget().getName().isMember()) {
            JlsName targetName = link.getTarget().getName();
            JlsName typeName = targetName.lastComponents(1);
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

    boolean isVoid(JlsName name) {
        return name.toString().equals("void");
    }

    boolean isPrimitive(JlsName name) {
        return primitives.contains(name.toString());
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

        if (extLink.isWebLink()) {
            String moduleName = extLink.getPackageToModule().get(packageName);
            URI uri = extLink.getUri();
            // uri = uri.resolve(moduleName);
            String path = moduleName + "/" + packageName.replace(".", "/") + "/";
            if (!typeName.isEmpty()) {
                uri = uri.resolve(path + typeName + ".html");
            } else {
                uri = uri.resolve(path);
            }
            link.setUri(uri);
            link.setResolved(true);
        } else {
            String up = Relativizer.relativize(ctx.getPackageName(), "", ctx.getCommonBasePath());
            // Relativizer.setFlattenedDirectories(extLink.getSemanticModel().commonBase());

            // TODO: This is wrong, it needs to get the common base path of the target
            String targetCommonBase = ctx.getCommonBasePath();

            String down = Relativizer.relativize("", packageName, targetCommonBase);
            // Relativizer.setFlattenedDirectories(api.commonBase());

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

    // /// Computes a relative path from the current package context, considering sibling modules.
    // /// @param from The source package name.
    // /// @param to The target package name.
    // /// @return The relative path string including sibling module base if applicable.
    // static String relativizeWithSiblingModule(String from, String to, String toModule) {
    //     String originCommonBase = ctx.getCommonBasePath();
    //     String toRoot = Relativizer.relativize(from, "");
    //     String toTarget = Relativizer.relativize("", to);
    //     Path path = Path.of(toRoot, "..", toModule, toTarget);
    //     return path.toString();
    // }

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
        boolean success = false;
        if (link.getTarget().getName() != null) {
            if (link.getTarget().getName().isMember()) {
                success = resolveMember(link);
            } else {
                success = resolvePackageOrType(link);
            }
        } else {
            success = resolveModule(link);
        }
        return success;
    }

    private boolean resolveMember(Link link) {
        String nameString = link.getTarget().getName().simpleName();
        if (ctx.getTypeName() != null && !ctx.getTypeName().isEmpty()) {
            TypeNode typeNode = api.getTypeNode(ctx.getTypeName());
            for (MethodNode methodNode : typeNode.getMethods()) {
                String methodName = methodNode.getName().simpleName();
                if (methodName.equals(nameString)) {
                    return true;
                }
            }
        }
        return false;
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
        if (siblingModulePath != null && siblingModulePath.hasModule(target.getModuleName())) {
            Scope scope = Scope.SIBLING;
            URI uri = relativize(link.getOrigin().getName(), target.getName(), target.getModuleName());
            link.setUri(uri);
            resolvedModule(link, scope);
            return true;
        } else {
            if (jreNamedModules.containsKey(target.getModuleName())) {
                Scope scope = Scope.STANDARD;
                // } else {
                    resolvedModule(link, scope);
                    String url = JAVA_24_URL + target.getModuleName() + "/module-summary.html";
                    link.setUri(URI.create(url));
                // }
                return true;
            }
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
            for (ModuleNode moduleNode : extLink.getSemanticModel().getModules()) {
                if (moduleNode.getName().toString().equals(target.getModuleName())) {
                    resolvedModule(link, extLink.isWebLink() ? Scope.EXTERNAL : Scope.SIBLING);

                    if (target.getName() == null && link.getOrigin() != null &&  link.getOrigin().getName() == null) {
                        // Module to module link
                        try {
                            link.setUri(new URI("../" + target.getModuleName()));
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }
                    } else {
                        link.setUri(relativize(link.getOrigin().getName(), null, target.getModuleName()));
                    }
                    return true;
                }
            }
        }
        return false;
    }

    boolean tryResolveJreType(Link link) {
        Reference target = link.getTarget();
        String qualifiedBinaryName = target.getName().fullyQualifiedJvmBinaryName();
        Class<?> jreType = JreUtil.loadClass(qualifiedBinaryName);
        if (jreType != null) {
            String typeName = link.getTarget().getName().fullyQualifiedName();
            if (siblingModulePath != null && siblingModulePath.hasClass(typeName)) {
                return tryResolveSiblingType(link);
            }

            resolvedType(link, Scope.STANDARD);
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

    boolean tryResolveSiblingType(Link link) {
        Reference target = link.getTarget();
        String typeName = link.getTarget().getName().fullyQualifiedName();
        Reference origin = link.getOrigin();
        JlsName originName = null;

        if (origin.getName() != null) {
            if (origin.getModuleName() == null) {
                throw new IllegalStateException("Origin has no module name");
            }
            String originCommonBase = ctx.getCommonBasePath(origin.getModuleName());
            originName = flattenDirectory(origin.getName().packageName(), originCommonBase);
        }

        String siblingModuleName = siblingModulePath.getModuleForClass(typeName);
        URI uri = getSiblingPackageRelativePath(originName, target, siblingModuleName);

        resolvedType(link, Scope.SIBLING);
        link.setUri(uri);
        return true;
    }

    private URI getSiblingPackageRelativePath(JlsName originName, Reference target, String siblingModuleName) {
        Path outputPath = Path.of(ctx.getOutputDirectory());
        Path siblingModuleDocPath = outputPath.resolve(siblingModuleName);
        JlsName siblingTypeName;

        if (Configuration.getFlattenPackages()) {
            // Get the element-list file from the sibling's doc output dir
            // and work out what its flattenedDirectories should be.
            String siblingFlattenedDirectories = getSiblingFlattenedDirectories(siblingModuleDocPath);
            siblingTypeName = flattenDirectory(target.getName(), siblingFlattenedDirectories);
        } else {
            siblingTypeName = target.getName();
        }

        String siblingPackageDir = siblingTypeName.toString().replaceAll("\\.", "/");

        URI siblingModuleLinkUri = relativize(originName, null, siblingModuleName);
        String siblingPackageRelativePath = siblingModuleLinkUri.toString() + "/" + siblingPackageDir;

        return URI.create(siblingPackageRelativePath);
    }

    private String getSiblingFlattenedDirectories(Path siblingModuleDocPath) {
        List<String> elementList = getSiblingElementList(siblingModuleDocPath);
        String siblingFlattenedDirectories = commonBase(elementList);
        return siblingFlattenedDirectories;
    }

    private List<String> getSiblingElementList(Path docRoot) {
        List<String> list = new ArrayList<>();
        Path elementListPath = docRoot.resolve("element-list");
        if (!Files.exists(elementListPath)) {
            return list;
        }
        try {
            return Files.readAllLines(elementListPath);
        } catch (IOException e) {
            return list;
        }
    }

    public String commonBase(List<String> elementList) {
        if (elementList == null || elementList.isEmpty()) {
           return "";
        } else {
            int lastDot = 0;
            String base = null;
            for(String pkgName : elementList) {
                if (pkgName.startsWith("module:")) {
                    continue;
                }
                if (base == null) {
                    base = pkgName;
                }
                for(int j = 0; j < Math.min(base.length(), pkgName.length()); ++j) {
                    if (base.charAt(j) != pkgName.charAt(j)) {
                        base = base.substring(0, lastDot);
                        break;
                    }
                    if (j == pkgName.length() - 1) {
                        base = base.substring(0, lastDot);
                    }
                    if (j < base.length() && base.charAt(j) == '.') {
                        lastDot = j;
                    }
                }
            }
            return base;
        }
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

            // typeNode's name potentially has more metadata than target's name
            target.setName(typeNode.getName());
            // TODO: Should we do this for every link being resolved?

            String module = "";
            if (!target.getModuleName().equals(origin.getModuleName())) {
                module = target.getModuleName();
            }
            JlsName originName = null;
            if (origin.getName() != null) {
                if (origin.getModuleName() == null) {
                    throw new IllegalStateException("Origin has no module name");
                }
                String originCommonBase = ctx.getCommonBasePath(origin.getModuleName());
                originName = flattenDirectory(origin.getName().packageName(), originCommonBase);
            }

            if (target.getModuleName() == null) {
                throw new IllegalStateException("Target has no module name");
            }
            String targetCommonBase = ctx.getCommonBasePath(target.getModuleName());
            JlsName targetName = flattenDirectory(typeNode.getName(), targetCommonBase);
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
            if (target.getModuleName() == null) {
                throw new IllegalStateException("Target has no module name");
            }
            String targetCommonBase = ctx.getCommonBasePath(target.getModuleName());
            JlsName targetName = flattenDirectory(target.getName(), targetCommonBase);
            if (origin.getName() == null) {
                // Origin is a module
                link.setUri(relativize(null, targetName, module));
            } else {
                if (origin.getModuleName() == null) {
                    throw new IllegalStateException("Origin has no module name");
                }
                String originCommonBase = ctx.getCommonBasePath(origin.getModuleName());
                JlsName originName = flattenDirectory(origin.getName().packageName(), originCommonBase);
                link.setUri(relativize(originName, targetName, module));
            }
            resolvedPackage(link, Link.Scope.LOCAL);
            return true;
        }
        return false;
    }

    boolean tryResolveExternalPackageOrType(Link link) {
        Reference target = link.getTarget();
        JlsName targetName = target.getName();

        for (int i=targetName.componentCount(); i>0; i--) {
            JlsName candidateName = targetName.firstComponents(i);
            String candidate = candidateName.toString();
            for (ExternalLink extLink : externalLinks) {
                if (extLink.getPackages().contains(candidate)) {
                    String packageName = candidate;
                    String typeName = targetName.simpleName();
                    // if (i < candidateName.packageComponentCount()) {
                    //     typeName = candidateName.lastComponents(-i).toString();
                    // } else {
                    //     typeName = "";
                    // }


                    // if (targetName.toString().contains("Observable")) {
                    //     System.out.println("Debug");
                    // }


                    resolvedExternal(link, extLink, packageName, typeName);
                    return true;
                }
            }
        }
        return false;
    }

    URI relativize(JlsName from, JlsName to, String module) {
        // a.b.C.D
        URI uri;
        int commonCount = 0;
        try {
            if (to == null) {
                if (from == null) {
                    // uri = new URI("../");
                    uri = new URI("");
                } else {
                    uri = new URI("../".repeat(from.componentCount()));
                }
            } else {
                if (from == null) {
                    // Origin is a module, not a package
                    commonCount = 0;
                    uri = null;
                } else {
                    // Origin is a package
                    commonCount = from.commonComponentCount(to);
                    uri = new URI("../".repeat(from.componentCount() - commonCount));
                }
            }
            if (module != null && !module.isEmpty()) {
                if (uri != null) {
                    uri = uri.resolve("../" + module + "/");
                } else {
                    uri = new URI("../" + module + "/"); // =======================HERE
                }
            }
            if (to != null) {
                JlsName pkgName = to.packageName();
                String toPackageNameString;
                if (pkgName.componentCount() == commonCount) {
                    toPackageNameString = "";
                } else {
                    JlsName name = to.packageName().lastComponents(-commonCount);
                    toPackageNameString = name.toString();
                }
                JlsName toTypeName = to.typeName();
                if (!toPackageNameString.isEmpty()) {
                    String toPackage = toPackageNameString.replace(".", "/");
                    if (uri == null) {
                        // Origin is not a package, or no target module was given
                        uri = new URI(toPackage + "/");
                    } else {
                        // Origin is a package, or target module was given
                        uri = uri.resolve(toPackage + "/");
                    }
                }
                if (!toTypeName.isEmpty()) {
                    String toType = to.binaryName();
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
    JlsName flattenDirectory(JlsName path, String flattenedDirectories) {
        if (path.isEmpty()) {
            return path;
        }
        if (flattenedDirectories != null && !flattenedDirectories.isEmpty()) {
            if (!path.toString().startsWith(flattenedDirectories)) {
                // Unrelated package
                return path;
            }
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
        JlsName name = ref.getName();
        if (name == null || name.isEmpty()) {
            // TODO: Links to modules end up here. This is fine.
            // Make sure the caller handes them though.
            return false;
        }
        if (qualifyLocalReference(ref)) return true;
        if (qualifyJreReference(ref)) return true;
        return qualifyExternalReference(ref);
    }

    private boolean qualifyLocalReference(Reference ref) {
        JlsName name = ref.getName();
        for (int i = name.componentCount(); i>0; i--) {
            JlsName candidate = name.firstComponents(i);
            PackageNode packageNode = api.getPackageNode(candidate);
            if (packageNode != null) {
                name.setPackageComponentCount(i);
                if (i < name.componentCount()) {
                    // Part of `name` is a package name
                    TypeNode typeNode = api.getTypeNode(name);
                    if (typeNode == null) {
                        if (api.getTypeNode(name.firstComponents(-1)) != null) {
                            // It is a member
                            name.setMember(true);
                            ref.setModule(packageNode.getModuleName());
                            return true;
                        } else {
                            // It wasn't found
                        }
                    } else {
                        // It is a type name
                        name.setKind(JlsName.Kind.TYPE);
                        ref.setModule(typeNode.getModuleName());
                        return true;
                    }
                } else {
                    // It is a package name
                    name.setKind(JlsName.Kind.PACKAGE);
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
                ref.setModule(typeNode.getModuleName());
                return true;
            }
        }
        return false;
    }

    private boolean qualifyJreReference(Reference ref) {
        JlsName name = ref.getName();
        Class<?> jreType = JreUtil.loadClass(name.fullyQualifiedName());
        if (jreType == null) {
            // Check if the last component of the name is a member
            JlsName candidate = name.lastComponents(1);
            jreType = JreUtil.loadClass(candidate.fullyQualifiedName());
            if (jreType != null) {
                name.setMember(true);
            }
        }
        if (jreType!= null) {
            JlsName packageName = NameUtil.createName(jreType.getPackageName());
            name.setPackageComponentCount(packageName.componentCount());
            if (name.componentCount() == packageName.componentCount()) {
                name.setKind(JlsName.Kind.PACKAGE);
            } else {
                if (!name.isMember()) {
                    name.setKind(JlsName.Kind.TYPE);
                }
            }
            ref.setModule(jreType.getModule().getName());
            return true;
        }
        return false;
    }

    private boolean qualifyExternalReference(Reference ref) {
        JlsName name = ref.getName();
        Pair<String,String> moduleAndPackage = findExternalPackage(name);
        if (moduleAndPackage != null) {
            String module = moduleAndPackage.getL();
            String pkg = moduleAndPackage.getR();
            JlsName packageName = NameUtil.createName(pkg);
            name.setPackageComponentCount(packageName.componentCount());
            ref.setModule(module);
            return true;
        }
        return false;
    }

    private Pair<String,String> findExternalPackage(JlsName targetName) {
        for (int i=targetName.componentCount(); i>0; i--) {
            JlsName candidateName = targetName.firstComponents(i);
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

