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
    private static final List<String> primitives = Arrays.asList("boolean","byte","char","short","int","long","float","double");
    private static String currentModuleName = "";
    private static String currentPackageName = "";
    private static HashMap<String,String> nativeModuleNames = new HashMap<>();
    private static HashMap<String,String> nativePackageNames = new HashMap<>();
    private static HashMap<String,String> suffix = new HashMap<>();
    private static final ModuleLayer moduleLayer = ModuleLayer.boot();
    private static Api api = null;
    private static String FlattenedDirectories = null;

    private LinkResolver() {
        // This hides the public constructor
    }

    public static void setApi(Api a) {
        api = a;
    }

    public static void setFlattenedDirectories(String sd) {
        FlattenedDirectories = sd;
    }

    public static void setCurrentPackageName(String name) {
        currentPackageName = name;
    }

    public static void setCurrentModuleName(String name) {
        currentModuleName = name;
    }

    public static void addNativeModule(String moduleName, String baseUrl, String s) {
        Optional<Module> module = moduleLayer.findModule(moduleName);
        if (module.isPresent()) {
            nativeModuleNames.put(moduleName, baseUrl);
            for (String packageName : module.get().getPackages()) {
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
        Reference primitiveOrVoid = resolvePrimitiveOrVoid(to);
        if (primitiveOrVoid != null) {
            return primitiveOrVoid;
        }
        Reference link = resolveModule(to);
        if (link.getKind() != Reference.Kind.NONE) {
            return link;
        }
        link = resolveNative(to);
        if (link.getKind() != Reference.Kind.NONE) {
            return link;
        }
        String[] qualified = qualify(to);
        String toPackageName = qualified[0];
        String toClassName = qualified[1];
        if (toPackageName.isEmpty()) {
            Configuration.getReporter().print(Diagnostic.Kind.WARNING, "Reference not found: " + original);
            link.setKind(Reference.Kind.NONE);
            return link;
        }
        String fromPackageName = getPackageName(from);
        link.setUri(relativizeWithModules(fromPackageName, toPackageName));
        link.setKind(Reference.Kind.PACKAGE);
        link.setScope(Reference.Scope.LOCAL);
        if (!toClassName.isEmpty()) {
            if (link.getUri() == null) {
                link.setKind(Reference.Kind.NONE);
            } else {
                if (!link.getUri().isEmpty()) link.setUri(link.getUri() + "/");
                link.setUri(link.getUri() + toClassName);
                link.setKind(Reference.Kind.TYPE);
            }
        }
        return link;
    }

    private static Reference resolvePrimitiveOrVoid(String to) {
        if (to == null || to.isEmpty() || to.equals("?") || to.indexOf("<") > -1) {
            return new Reference();
        }
        if ("void".equals(to) || "Void".equals(to)){
            return new Reference(Reference.Scope.NATIVE, Reference.Kind.VOID, to);
        }
        if (primitives.contains(to)){
            return new Reference(Reference.Scope.NATIVE, Reference.Kind.PRIMITIVE, to);
        }
        return null;
    }

    // Returns [toPackageName, toClassName, qualifiedTo]
    private static String[] qualify(String name) {
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

    public static Reference resolveNative(String to) {
        int dot = 0;
        do {
            dot = to.indexOf(".", dot + 1);
            if (dot != -1) {
                String id = to.substring(0, dot);
                String baseUrl = nativePackageNames.get(id);
                if (baseUrl != null) {
                    String toPackage = getPackageName(to);
                    String toClass = getClassName(to);
                    String uri = baseUrl + "/" + toPackage.replace(".", "/") + "/" + toClass+ suffix.get(id);
                    return new Reference(Reference.Scope.NATIVE, Reference.Kind.URL, to, uri);
                }
            }
        } while(dot != -1);
        return new Reference();
    }

    public static Reference resolveModule(String to) {
        ModuleNode moduleNode = getModule(to);
        if (moduleNode != null) {
            return new Reference(Reference.Scope.LOCAL, Reference.Kind.MODULE, to, "../" + to);
        }
        String baseUrl = nativeModuleNames.get(to);
        if (baseUrl != null) {
            return new Reference(Reference.Scope.NATIVE, Reference.Kind.URL, to, baseUrl + "/module-summary.html");
        }
        return new Reference();
    }

    public static ModuleNode getModule(String name) {
        for (ModuleNode moduleNode : api.getModules()) {
            if (moduleNode.getName().equals(name)) {
                return moduleNode;
            }
        }
        return null;
    }

    public static boolean isPackageQualified(String name) {
        return name.indexOf('.') > -1;
    }

    public static String qualifyClass(String name) {
        // 'from' could be used to ensure the closest matching class is chosen
        for (PackageMember member : api.getClasses()) {
            if (member instanceof ClassNode classNode && classNode.getSimpleName().equals(name)) {
                return classNode.getQualifiedName();
            }
        }
        return "";
    }

    public static String qualifyPackage(String to) {
        // 'from' could be used to ensure the closest matching package is chosen
        for (PackageNode node : api.getPackages()) {
            int p = node.getName().lastIndexOf(".");
            if (node.getName().substring(p + 1).equals(to)) {
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
        ModuleNode toModule = toPackage.getModule();
        ModuleNode fromModule = api.getModuleNode(currentModuleName);
        if (toModule == null) {
            Configuration.getReporter().print(Diagnostic.Kind.ERROR, "Error resolving module for: " + to);
            return null;
        }
        if (fromModule == null) {
            Configuration.getReporter().print(Diagnostic.Kind.ERROR, "Error resolving module for: " + from);
            return null;
        }
        if (fromModule.getName().equals(toModule.getName())) {
            // to and from are members of the same module
            return relativize(from, to);
        } else {
            String packageRoot = relativize(from, "");
            String toModulePath = packageRoot + "../" + toModule.getName();
            String toPackagePath = relativize("", to);
            return toModulePath + "/" + toPackagePath;
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
        // int p = 0;
        // if (!from.isEmpty()) {
        //     for (p=fromParts.length-1; p>=0; p--) {
        //         if (p<toParts.length &&fromParts[p].equals(toParts[p])) {
        //             p++;
        //             break;
        //         }
        //         if (!rel.isEmpty()) rel.append("/");
        //         rel.append("..");
        //     }
        // }

        // if (p == -1) p = 0;
        // for (;p < toParts.length; p++) {
        //     if (!rel.isEmpty()) rel.append("/");
        //     rel.append(toParts[p]);
        // }
        if (!from.isEmpty()) {
            appendParentDirs(rel, fromParts.length - commonIndex);
        }
        appendTargetDirs(rel, toParts, commonIndex);
        return rel.toString();
    }

    private static String flattenDirectory(String path) {
        if (FlattenedDirectories != null && !FlattenedDirectories.isEmpty() && path.startsWith(FlattenedDirectories)) {
            return path.substring(FlattenedDirectories.length() + 1);
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
            if (rel.length() > 0) rel.append("/");
            rel.append("..");
        }
    }

    private static void appendTargetDirs(StringBuilder rel, String[] toParts, int start) {
        for (int i = start; i < toParts.length; i++) {
            if (toParts[i].isEmpty()) continue;
            if (rel.length() > 0) rel.append("/");
            rel.append(toParts[i]);
        }
    }
}
