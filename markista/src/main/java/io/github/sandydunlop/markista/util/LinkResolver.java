package io.github.sandydunlop.markista.util;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.List;

import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ClassNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageMember;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.Reference;
import io.github.sandydunlop.markista.model.TypeNode;

/// `LinkResolver` calculates the paths for Markdown documents
/// to link between different packages and to URLs of external
/// packages and their contents.
///
/// This class manages resolving references and generating links
/// for types, packages, modules, and native Java elements.
///
/// It supports resolving primitives, void, native Java modules and packages,
/// and also local API model packages and types.
///
/// LinkResolver must be initialized for the current API before use via `init(Api)`.
/// It provides multiple resolve methods for building appropriate links.
///
/// The class supports relative path calculation for Markdown output.
/// It also manages native Java documentation URLs for modules and packages.
public class LinkResolver {
    /// The Context singleton instance providing access to the current documentation generation context,
    /// including configuration, current module/package/type names, and reporting utilities.
    private static Context ctx;
    private static final String DOT_HTML = ".html";
    private static final String JAVA_24_URL = "https://docs.oracle.com/en/java/javase/24/docs/api/";
    private static final List<String> primitives = Arrays.asList("boolean","byte","char","short","int","long","float","double");
    private static final ModuleLayer moduleLayer = ModuleLayer.boot();
    private static HashMap<String,String> nativeModuleNames = new HashMap<>();
    private static HashMap<String,String> nativePackageNames = new HashMap<>();
    private static HashMap<String,String> suffix = new HashMap<>();

    /// The Api model representing the entire documented API structure,
    /// including modules, packages, types, and members used for cross-referencing and navigation.
    private static Api api = null;

    private static String flattenedDirectories = null;

    /// The default constructor is private to prevent instantiation.
    private LinkResolver() {
        // This hides the public constructor
    }

    /// Initializes the LinkResolver for the API being documented.
    /// @param a The API being documented.
    public static void init(Api a) {
        api = a;
        nativeModuleNames = new HashMap<>();
        nativePackageNames = new HashMap<>();
        suffix = new HashMap<>();
        ctx = Context.getInstance();
    }

    /// Sets the string used to adjust flattened directories in relative path calculations.
    /// @param sd The string representing flattened directories.
    public static void setFlattenedDirectories(String sd) {
        flattenedDirectories = sd;
    }

    /// Adds a native module URL base for linking native Java modules and their packages.
    /// @param moduleName The native module's name (e.g., java.base).
    /// @param baseUrl The base URL for the native module's documentation.
    /// @param s The suffix to append to URLs for this module's packages (e.g., ".html").
    public static void addNativeModuleUrl(String moduleName, String baseUrl, String s) {
        Optional<Module> module = moduleLayer.findModule(moduleName);
        if (module.isPresent()) {
            nativeModuleNames.put(moduleName, baseUrl);
            for (String packageName : module.get().getPackages()) {
                nativePackageNames.put(packageName, baseUrl);
                suffix.put(packageName, s);
            }
        }
    }

    /// Extracts the package name part from an identifier string, assuming lowercase start for package.
    /// @param id The identifier string (e.g., "java.lang.String").
    /// @return The package name portion or empty string if not a package.
    private static String getPackageName(String id) {
        if (id == null || id.isEmpty() || Character.isUpperCase(id.charAt(0))) {
            return "";
        }
        int dot;
        for (dot = 0; dot < id.length() && !Character.isUpperCase(id.charAt(dot)); dot++);
        String packageName = id;
        if (dot > 0 && dot < id.length()) {
            return packageName.substring(0, dot - 1);
        }
        return id;
    }

    /// Extracts the class name part from an identifier string, assuming uppercase start for class.
    /// @param id The identifier string.
    /// @return The class name portion or empty string if none found.
    private static String getClassName(String id) {
        if (id == null || id.isEmpty()) return "";
        if (Character.isUpperCase(id.charAt(0))) {
            return id;
        }
        int dot;
        for (dot = 0; dot < id.length() && !Character.isUpperCase(id.charAt(dot)); dot++) {
            // Looking for class name
        }
        String className = id;
        if (dot > 0 && dot < id.length()) {
            return className.substring(dot);
        }
        return "";
    }

    /// Resolves a reference from the current package context to a target type or package.
    /// @param to The identifier to resolve.
    /// @return A Reference object representing the resolved link or special kind.
    public static Reference resolve(String to) {
        return resolve(ctx.getPackageName(), to);
    }

    /// Resolves a reference from a specific source package to a target identifier.
    /// Tries multiple strategies: unsupported, primitive/void, native, local, module.
    /// @param from The package name the reference originates from.
    /// @param to The target identifier to resolve.
    /// @return A Reference object representing the resolved link or special kind.
    public static Reference resolve(String from, String to) {
        String original = to;
        if (to == null || to.isEmpty()) return new Reference();
        Reference link = resolveUnsupported(to);
        if (link.getKind() != Reference.Kind.NONE) {
            return link;
        }
        link = resolvePrimitiveOrVoid(to);
        if (link.getKind() != Reference.Kind.NONE) {
            return link;
        }

        String[] qualified = qualifyType(to);
        String toPackageName = qualified[0];
        String toClassName = qualified[1];
        if (toPackageName.isEmpty()) {
            ctx.reportWarning("Reference not found: " + original);
            link.setKind(Reference.Kind.NONE);
            return link;
        }

        link = resolveNativePackageOrType(toPackageName, toClassName);
        if (link.getKind() != Reference.Kind.NONE) {
            return link;
        }

        link = resolveLocalPackageOrType(from, toPackageName, toClassName);
        if (link.getKind() != Reference.Kind.NONE) {
            return link;
        }

        link = resolveModule(from, to);
        if (link.getKind() != Reference.Kind.NONE) {
            return link;
        }

        return link;
    }

    /// Checks if the target is unsupported by the `LinkResolver`.
    /// @param target The target to be resolved.
    /// @return A `Reference` with `Reference.Kind.UNKNOWN` if unsupported, else `Reference.Kind.NONE`.
    private static Reference resolveUnsupported(String target) {
        if (target.equals("?") || target.indexOf("<") > -1) {
            return new Reference(Reference.Scope.UNKNOWN, Reference.Kind.UNKNOWN, target);
        }
        return new Reference();
    }

    /// Checks if the target is a primitive type or void.
    /// @param target The target to be resolved.
    /// @return A Reference with appropriate kind if primitive or void, else none.
    private static Reference resolvePrimitiveOrVoid(String target) {
        if ("void".equals(target) || "Void".equals(target)){
            return new Reference(Reference.Scope.NATIVE, Reference.Kind.VOID, target);
        }
        if (primitives.contains(target)){
            return new Reference(Reference.Scope.NATIVE, Reference.Kind.PRIMITIVE, target);
        }
        return new Reference();
    }

    /// Returns canonical package and class names for a given type name, ensuring both parts are qualified.
    /// @param name The type name to qualify.
    /// @return A String array: [packageName, className].
    public static String[] qualifyType(String name) {
        String toPackageName = getPackageName(name);
        String toClassName = getClassName(name);
        if (toPackageName.isEmpty()) {
            String qualifiedTo = qualifyClass(toClassName);
            toPackageName = getPackageName(qualifiedTo);
            toClassName = getClassName(qualifiedTo);
        }
        if (!isPackageQualified(toPackageName)) {
            String qualifiedTo = qualifyPackage(toPackageName);
            toPackageName = getPackageName(qualifiedTo);
            toClassName = getClassName(qualifiedTo);
        }
        return new String[] { toPackageName, toClassName };
    }

    /// Resolves a native Java package or type to an external documentation URL.
    /// @param toPackageName The package name.
    /// @param toClassName The class name within the package.
    /// @return A Reference with scope NATIVE and kind URL if native, else none.
    public static Reference resolveNativePackageOrType(String toPackageName, String toClassName) {
        Reference link = new Reference();
        String baseUrl = nativePackageNames.get(toPackageName);
        if (baseUrl != null) {
            link.setDisplayName(toPackageName);
            link.setScope(Reference.Scope.NATIVE);
            link.setKind(Reference.Kind.URL);
            String uri = baseUrl + "/" + toPackageName.replace(".", "/");
            if (!toClassName.isEmpty()) {
                if (!link.getUri().isEmpty()) link.setUri(link.getUri() + "/");
                uri = uri + "/" + toClassName;
                link.setDisplayName(toPackageName + "." + toClassName);
                link.setClassName(toPackageName + "." + toClassName);
            }
            uri += suffix.get(toPackageName);
            link.setUri(uri);
        }
        return link;
    }

    /// Resolves a local package or type within the documented API to a relative link.
    /// @param from The originating package name.
    /// @param toPackageName The target package name.
    /// @param toClassName The target class name.
    /// @return A Reference with scope LOCAL and kind MODULE or TYPE, or none if unresolved.
    public static Reference resolveLocalPackageOrType(String from, String toPackageName, String toClassName) {
        Reference link = new Reference();
        String fromPackageName = getPackageName(from);
        if (getPackageName(toPackageName) == null) {
            return link;
        }
        if (!toClassName.isEmpty()) {
            String qualifiedClassName = toPackageName + "." + toClassName;
            TypeNode typeNode = api.getTypeNode(qualifiedClassName);
            if (typeNode == null) {
                ctx.reportError("Unable to resolve type: " + qualifiedClassName);
                return link;
            }
        }
        PackageNode packageNode = api.getPackageNode(toPackageName);
        if (packageNode == null) return link;
        link.setDisplayName(toPackageName);
        link.setScope(Reference.Scope.LOCAL);
        link.setKind(Reference.Kind.MODULE);
        link.setUri(relativizeWithModules(fromPackageName, toPackageName));
        if (!toClassName.isEmpty()) {
            link.setDisplayName(toPackageName + "." + toClassName);
            link.setClassName(toPackageName + "." + toClassName);
            addClassToReference(toClassName, link);
        }
        return link;
    }

    /// Appends the class name to the URI on the Reference and sets kind TYPE if not URL.
    /// @param className The class name to add.
    /// @param link The Reference object to modify.
    private static void addClassToReference(String className, Reference link) {
        if (!link.getUri().isEmpty()) link.setUri(link.getUri() + "/");
        link.setUri(link.getUri() + className);
        if (link.getKind() != Reference.Kind.URL) {
            link.setKind(Reference.Kind.TYPE);
        }
    }

    /// Resolves a module reference by name to a Reference either local or native.
    /// @param from The package name from which the link originates.
    /// @param target The module name to resolve.
    /// @return A Reference with scope LOCAL or NATIVE with appropriate URI or none if unresolved.
    public static Reference resolveModule(String from, String target) {
        String apiRoot = relativize(from, "");
        ModuleNode moduleNode = getModule(target);
        Path path = Path.of(apiRoot, "..", target);
        if (moduleNode != null) {
            return new Reference(Reference.Scope.LOCAL, Reference.Kind.MODULE, target, path.toString());
        }
        String baseUrl = nativeModuleNames.get(target);
        if (baseUrl != null) {
            return new Reference(Reference.Scope.NATIVE, Reference.Kind.URL, path.toString(), baseUrl + "/module-summary.html");
        }
        return new Reference();
    }

    /// Returns the ModuleNode for the named module in the current API.
    /// @param moduleName The module's name.
    /// @return The ModuleNode if found, else null.
    public static ModuleNode getModule(String moduleName) {
        for (ModuleNode moduleNode : api.getModules()) {
            if (moduleNode.getName().equals(moduleName)) {
                return moduleNode;
            }
        }
        return null;
    }

    /// Checks if a package name is qualified (contains a dot).
    /// @param name The package name to check.
    /// @return True if the name is qualified, false otherwise.
    public static boolean isPackageQualified(String name) {
        return name.indexOf('.') > -1;
    }

    /// Qualifies a simple class name to its fully qualified name within the API, if exists.
    /// @param simpleName The class simple name.
    /// @return The fully qualified class name or empty string if not found.
    public static String qualifyClass(String simpleName) {
        for (PackageMember member : api.getClasses()) {
            if (member instanceof ClassNode classNode && classNode.getSimpleName().equals(simpleName)) {
                return classNode.getQualifiedName();
            }
        }
        return "";
    }

    /// Qualifies a simple package name to its fully qualified name within the API, if exists.
    /// @param simpleName The package simple name.
    /// @return The fully qualified package name or empty string if not found.
    public static String qualifyPackage(String simpleName) {
        for (PackageNode node : api.getPackages()) {
            int p = node.getName().lastIndexOf(".");
            if (node.getName().substring(p + 1).equals(simpleName)) {
                return node.getName();
            }
        }
        return "";
    }

    /// Produces a relative path from the current package context to a target package.
    /// @param to The target package name.
    /// @return A relative filesystem path string.
    public static String relativize(String to) {
        return relativize(ctx.getPackageName(), to);
    }

    /// Produces a relative path considering modules between two packages.
    /// If packages belong to different modules, the relative path includes module directories.
    /// @param from The package name for the source.
    /// @param to The package name for the target.
    /// @return The relative path string.
    public static String relativizeWithModules(String from, String to) {
        PackageNode toPackage = api.getPackageNode(to);
        if (toPackage == null) {
            ctx.reportError("Error resolving package for: " + to);
            return null;
        }
        String fromModuleName = "";
        String toModuleName = "";
        ModuleNode fromModule = api.getModuleNode(ctx.getModuleName());
        ModuleNode toModule = toPackage.getModule();
        if (fromModule != null) {
            fromModuleName = fromModule.getName();
        }
        if (toModule != null) {
            toModuleName = toModule.getName();
        }
        if (fromModuleName.equals(toModuleName)) {
            // to and from are members of the same module
            return relativize(from, to);
        } else {
            return Path.of(relativize(from, ""), toModuleName, relativize("", to)).toString();
        }
    }

    /// Produces a relative path string from one package to another by splitting and comparing components.
    /// Supports flattened directories if set.
    /// @param from The source package name.
    /// @param to The target package name.
    /// @return The relative path string.
    public static String relativize(String from, String to) {
        if (from == null || to == null) return null;
        from = flattenDirectory(from);
        to = flattenDirectory(to);
        String[] fromParts = from.split("\\.");
        String[] toParts = to.split("\\.");
        int commonIndex = findCommonIndex(fromParts, toParts);
        StringBuilder rel = new StringBuilder();
        if (!from.isEmpty()) {
            appendParentDirs(rel, fromParts.length - commonIndex);
        }
        appendTargetDirs(rel, toParts, commonIndex);
        return rel.toString();
    }

    /// Removes prefix directories from a path if flattenedDirectories is set and matches.
    /// @param path The package name or path to flatten.
    /// @return The adjusted path or original if no flattening applies.
    private static String flattenDirectory(String path) {
        if (flattenedDirectories != null && !flattenedDirectories.isEmpty() && path.startsWith(flattenedDirectories)) {
            if (path.length() <= flattenedDirectories.length()) {
                return "";
            }
            return path.substring(flattenedDirectories.length() + 1);
        }
        return path;
    }

    /// Finds the common prefix index between two string arrays.
    /// @param fromParts Array of strings for source path.
    /// @param toParts Array of strings for target path.
    /// @return The number of common leading segments.
    private static int findCommonIndex(String[] fromParts, String[] toParts) {
        if (fromParts.length == 0 || toParts.length == 0) {
            return 0;
        }
        int len = Math.min(fromParts.length, toParts.length);
        int i = 0;
        while (i < len && fromParts[i].equals(toParts[i])) {
            i++;
        }
        return i;
    }

    /// Appends parent directory segments (..) to the relative path string builder.
    /// @param rel The StringBuilder accumulating the path.
    /// @param count The number of parent directory segments to append.
    private static void appendParentDirs(StringBuilder rel, int count) {
        for (int i = 0; i < count; i++) {
            if (!rel.isEmpty()) rel.append("/");
            rel.append("..");
        }
    }

    /// Appends target directory segments to the relative path string builder starting at index start.
    /// @param rel The StringBuilder accumulating the path.
    /// @param toParts Array of target path segments.
    /// @param start The start index for appending segments.
    private static void appendTargetDirs(StringBuilder rel, String[] toParts, int start) {
        for (int i = start; i < toParts.length; i++) {
            if (toParts[i].isEmpty()) continue;
            if (!rel.isEmpty()) rel.append("/");
            rel.append(toParts[i]);
        }
    }

    /// Adds a native Java module URL for linking purposes using a standard Oracle Javadoc base URL.
    /// @param moduleName The native module name (e.g., java.base).
    private static void addNativeModule(String moduleName) {
        // Tell the link resolver what web address to find docs for certain Java modules at
        LinkResolver.addNativeModuleUrl(moduleName, JAVA_24_URL + moduleName, DOT_HTML);
    }

    /// Adds known native modules for Java SE 24 to the resolver.
    /// This populates internal mappings for native module and package documentation URLs.
    public static void addNativeModules() {
        addNativeModule("java.base");
        addNativeModule("java.compiler");
        addNativeModule("java.desktop");
        addNativeModule("java.instrument");
        addNativeModule("java.logging");
        addNativeModule("java.management");
        addNativeModule("java.management.rmi");
        addNativeModule("java.naming");
        addNativeModule("java.net.http");
        addNativeModule("java.prefs");
        addNativeModule("java.rmi");
        addNativeModule("java.scripting");
        addNativeModule("java.se");
        addNativeModule("java.security.jgss");
        addNativeModule("java.security.sasl");
        addNativeModule("java.smartcardio");
        addNativeModule("java.sql");
        addNativeModule("java.sql.rowset");
        addNativeModule("java.transaction.xa");
        addNativeModule("java.xml");
        addNativeModule("java.xml.crypto");
        addNativeModule("jdk.accessibility");
        addNativeModule("jdk.attach");
        addNativeModule("jdk.javadoc");
        addNativeModule("jdk.compiler");
        addNativeModule("jdk.crypto.cryptoki");
        addNativeModule("jdk.dynalink");
        addNativeModule("jdk.editpad");
        addNativeModule("jdk.hotspot.agent");
        addNativeModule("jdk.httpserver");
        addNativeModule("jdk.incubator.vector");
        addNativeModule("jdk.jartool");
        addNativeModule("jdk.javadoc");
        addNativeModule("jdk.jcmd");
        addNativeModule("jdk.jconsole");
        addNativeModule("jdk.jdeps");
        addNativeModule("jdk.jdi");
        addNativeModule("jdk.jdwp.agent");
        addNativeModule("jdk.jfr");
        addNativeModule("jdk.jlink");
        addNativeModule("jdk.jpackage");
        addNativeModule("jdk.jshell");
        addNativeModule("jdk.jsobject");
        addNativeModule("jdk.jstatd");
        addNativeModule("jdk.localedata");
        addNativeModule("jdk.management");
        addNativeModule("jdk.management.agent");
        addNativeModule("jdk.management.jfr");
        addNativeModule("jdk.naming.dns");
        addNativeModule("jdk.naming.rmi");
        addNativeModule("jdk.net");
        addNativeModule("jdk.nio.mapmode");
        addNativeModule("jdk.sctp");
        addNativeModule("jdk.security.auth");
        addNativeModule("jdk.security.jgss");
        addNativeModule("jdk.xml.dom");
        addNativeModule("jdk.zipfs");        
    }
}