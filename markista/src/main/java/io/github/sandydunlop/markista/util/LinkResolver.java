package io.github.sandydunlop.markista.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.List;

import javax.tools.Diagnostic;

import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ClassNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageMember;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.Reference;


/// This class woks calculates the paths for Markdown documents 
/// to link between different packages and to URLs of external
/// packages and their contents.
public class LinkResolver {
    private static final String DOT_HTML = ".html";
    private static final String JAVA_24_URL = "https://docs.oracle.com/en/java/javase/24/docs/api/";
    private static final List<String> primitives = Arrays.asList("boolean","byte","char","short","int","long","float","double");
    private static String currentModuleName = "";
    private static final ModuleLayer moduleLayer = ModuleLayer.boot();
    private static String currentPackageName = "";
    private static HashMap<String,String> nativeModuleNames = new HashMap<>();
    private static HashMap<String,String> nativePackageNames = new HashMap<>();
    private static HashMap<String,String> suffix = new HashMap<>();
    private static Api api = null;
    private static String flattenedDirectories = null;

    private LinkResolver() {
        // This hides the public constructor
    }

    public static void init(Api a) {
        api = a;
        nativeModuleNames = new HashMap<>();
        nativePackageNames = new HashMap<>();
        suffix = new HashMap<>();
        // if (Configuration.getCreateExternalLinks()) {
        //     addNativeModules();
        // }
    }

    public static void setFlattenedDirectories(String sd) {
        flattenedDirectories = sd;
    }

    public static void setCurrentPackageName(String name) {
        currentPackageName = name;
    }

    public static void setCurrentModuleName(String name) {
        currentModuleName = name;
    }

    public static void addNativeModuleUrl(String moduleName, String baseUrl, String s) {
        Optional<Module> module = moduleLayer.findModule(moduleName);
        if (module.isPresent()) {
            nativeModuleNames.put(moduleName, baseUrl);
            for (String packageName : module.get().getPackages()) {
                System.out.println("PKG: " + packageName);
                nativePackageNames.put(packageName, baseUrl);
                suffix.put(packageName, s);
            }
        }
    }

    private static String getPackageName(String id) {
        if (id == null || id.isEmpty() || Character.isUpperCase(id.charAt(0))) {
            return "";
        }
        int dot;
        for (dot=0; dot<id.length() && !Character.isUpperCase(id.charAt(dot)); dot++);
        String packageName = id;
        if (dot > 0 && dot < id.length()) {
            return packageName.substring(0, dot - 1);
        }
        return id;
    }

    private static String getClassName(String id) {
        if (id == null || id.isEmpty()) return "";
        if (Character.isUpperCase(id.charAt(0))) {
            return id;
        }
        int dot;
        for (dot=0; dot<id.length() && !Character.isUpperCase(id.charAt(dot)); dot++) {
            // Looking for class name
        }
        String className = id;
        if (dot > 0 && dot < id.length()) {
            return className.substring(dot);
        }
        return "";
    }

    public static Reference resolve(String to) {
        return resolve(currentPackageName, to);
    }

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
            Configuration.getReporter().print(Diagnostic.Kind.WARNING, "Reference not found: " + original);
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
    /// @param target The target to be resolved
    /// @return A `Reference` with `Reference.Kind.UNSUPPORTED` if the target
    ///         is unsupported, otherwise with `Reference.Kind.NONE`.
    private static Reference resolveUnsupported(String target) {
        if (target.equals("?") || target.indexOf("<") > -1) {
            return new Reference(Reference.Scope.UNKNOWN, Reference.Kind.UNKNOWN, target);
        }
        return new Reference();
    }

    /// Checks if the target is a primitive type or is void or the Void type.
    /// @param target The target to be resolved
    /// @return A `Reference` with `Reference.Kind.UNSUPPORTED` if the target
    ///         is a primitive type or is void or the Void type, otherwise
    ///         `Reference.Kind.NONE`.
    private static Reference resolvePrimitiveOrVoid(String target) {
        if ("void".equals(target) || "Void".equals(target)){
            return new Reference(Reference.Scope.NATIVE, Reference.Kind.VOID, target);
        }
        if (primitives.contains(target)){
            return new Reference(Reference.Scope.NATIVE, Reference.Kind.PRIMITIVE, target);
        }
        return new Reference();
    }

    /// Gets the canonical package name and class name of a class
    /// that is part of the API being documented.
    /// @param The name of a class
    /// @return An array containing the canonical name of the 
    ///         class's package, and the class name.
    private static String[] qualifyType(String name) {
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

    /// Checks if the target is native package. If it is a native package,
    /// the `Reference` bring returned will have `Reference.Type.URL` and
    /// its `uri` will be the URL of the Oracle Javadoc for the package.
    /// @param toPackageName The package name of the target to be resolved
    /// @param toClassName The class name of the target to be resolved
    /// @return A `Reference` with `Reference.Scope.NATIVE` if the target
    ///         is a native Java type, otherwise `Reference.Scope.NONE`.
    public static Reference resolveNativePackageOrType(String toPackageName, String toClassName) {
        Reference link = new Reference();
        // int dot = 0;
        // do {
        //     dot = toPackageName.indexOf(".", dot + 1);
        //     if (dot != -1) {
        //         String id = toPackageName.substring(0, dot);
                String baseUrl = nativePackageNames.get(toPackageName);
                if (baseUrl != null) {
                    link.setName(toPackageName);
                    link.setScope(Reference.Scope.NATIVE);
                    link.setKind(Reference.Kind.URL);
                    String uri = baseUrl + "/" + toPackageName.replace(".", "/");
                    if (!toClassName.isEmpty()) {
                        if (!link.getUri().isEmpty()) link.setUri(link.getUri() + "/");
                        uri = uri + "/" + toClassName;
                        link.setName(toClassName);
                    }
                    uri += suffix.get(toPackageName);
                    link.setUri(uri);
                    // return link;
                }else{
                    for (String key : nativePackageNames.keySet()) {
                        System.out.println("package: " + key);
                    }
                }
        //     }
        // } while(dot != -1);
        return link;
    }

    public static Reference resolveLocalPackageOrType(String from, String toPackageName, String toClassName) {
        String fromPackageName = getPackageName(from);
        Reference link = new Reference();
        PackageNode packageNode = api.getPackageNode(toPackageName);
        if (packageNode == null) return link;
        link.setScope(Reference.Scope.LOCAL);
        link.setKind(Reference.Kind.MODULE);
        link.setName(toPackageName);
        link.setUri(relativizeWithModules(fromPackageName, toPackageName));

        if (!toClassName.isEmpty()) {
            addClassToReference(toClassName, link);
        }

        return link;
    }

    private static void addClassToReference(String className, Reference link) {
        if (!link.getUri().isEmpty()) link.setUri(link.getUri() + "/");
        link.setName(className);
        link.setUri(link.getUri() + className);
        if (link.getKind() != Reference.Kind.URL) {
            link.setKind(Reference.Kind.TYPE);
        }
    }

    /// Gets a `Reference` for a package with `name` being the canonical
    /// name of the module.
    /// For packages defined in the API being documented, this will have `scope`
    /// `Reference.Scope.LOCAL` and `kind` `Reference.Kind.MODULE`, with `uri`
    /// being the relative path between the _from_ package and the _target_ package.
    /// For native Java modules, this will have `scope` `Reference.Scope.NATIVE`
    /// and `kind` `Reference.Kind.URL`, with its `uri` being set to the URL 
    /// of the Oracle Javadoc for the package.
    public static Reference resolveModule(String from, String target) {
        String apiRoot = relativize(from, "");
        ModuleNode moduleNode = getModule(target);
        if (moduleNode != null) {
            String path = FileUtils.joinPaths(apiRoot, "..");
            path = FileUtils.joinPaths(path, target);
            return new Reference(Reference.Scope.LOCAL, Reference.Kind.MODULE, target, path);
        }
        String baseUrl = nativeModuleNames.get(target);
        if (baseUrl != null) {
            String path = FileUtils.joinPaths(apiRoot, "..");
            path = FileUtils.joinPaths(path, target);
            return new Reference(Reference.Scope.NATIVE, Reference.Kind.URL, path, baseUrl + "/module-summary.html");
        }
        return new Reference();
    }

    /// Gets a module node by its name
    /// @param moduleName The module's name (eg java.base)
    /// @return a `ModuleNode` for the requested module
    public static ModuleNode getModule(String moduleName) {
        for (ModuleNode moduleNode : api.getModules()) {
            if (moduleNode.getName().equals(moduleName)) {
                return moduleNode;
            }
        }
        return null;
    }

    /// Checks if a package name is qualified (canonical) or unqualified.
    /// @param name A package name
    /// @return True if the name is a qualified name, false otherwise.
    public static boolean isPackageQualified(String name) {
        return name.indexOf('.') > -1;
    }

    /// Gets the canonical name of a class defined in the API being documented.
    /// @param simpleName The unqualified name of the class.
    /// @return The canonical name of the class, or an empty string
    ///         if the class wasn't found in the API model.
    public static String qualifyClass(String simpleName) {
        // 'from' could be used to ensure the closest matching class is chosen
        for (PackageMember member : api.getClasses()) {
            if (member instanceof ClassNode classNode && classNode.getSimpleName().equals(simpleName)) {
                return classNode.getQualifiedName();
            }
        }
        return "";
    }

    /// Returns the canonical name of a package defined in the API being documented.
    /// @param simpleName The unqualified name of the package.
    /// @return The canonical name of the package, or an empty string
    ///         if the package wasn't found in the API model.
    public static String qualifyPackage(String simpleName) {
        // 'from' could be used to ensure the closest matching package is chosen
        for (PackageNode node : api.getPackages()) {
            int p = node.getName().lastIndexOf(".");
            if (node.getName().substring(p + 1).equals(simpleName)) {
                return node.getName();
            }
        }
        return "";
    }

    public static String relativize(String to) {
        return relativize(currentPackageName, to);
    }

    public static String relativizeWithModules(String from, String to) {
        PackageNode toPackage = api.getPackageNode(to);
        if (toPackage == null) {
            Configuration.getReporter().print(Diagnostic.Kind.ERROR, "Error resolving package for: " + to);
            return null;
        }
        String fromModuleName = "";
        String toModuleName = "";
        ModuleNode fromModule = api.getModuleNode(currentModuleName);
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
            String rel = relativize(from, "");
            rel = FileUtils.joinPaths(rel, toModuleName);
            rel = FileUtils.joinPaths(rel, relativize("", to));
            return rel;
        }
    }

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

    private static String flattenDirectory(String path) {
        if (flattenedDirectories != null && !flattenedDirectories.isEmpty() && path.startsWith(flattenedDirectories)) {
            return path.substring(flattenedDirectories.length() + 1);
        }
        return path;
    }

    private static int findCommonIndex(String[] fromParts, String[] toParts) {
        if (fromParts.length ==0 || toParts.length == 0) {
            return 0;
        }
        int len = Math.min(fromParts.length, toParts.length);
        int i = 0;
        while (i < len && fromParts[i].equals(toParts[i])) {
            i++;
        }
        return i;
    }

    private static void appendParentDirs(StringBuilder rel, int count) {
        for (int i = 0; i < count; i++) {
            if (!rel.isEmpty()) rel.append("/");
            rel.append("..");
        }
    }

    private static void appendTargetDirs(StringBuilder rel, String[] toParts, int start) {
        for (int i = start; i < toParts.length; i++) {
            if (toParts[i].isEmpty()) continue;
            if (!rel.isEmpty()) rel.append("/");
            rel.append(toParts[i]);
        }
    }


    private static void addNativeModule(String moduleName) {
        // Tell the link resolver what web address to find docs for certain Java modules at
        LinkResolver.addNativeModuleUrl(moduleName, JAVA_24_URL + moduleName, DOT_HTML);
    }

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
