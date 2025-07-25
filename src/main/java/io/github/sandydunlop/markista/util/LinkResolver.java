package io.github.sandydunlop.markista.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.List;

import javax.tools.Diagnostic;

import io.github.sandydunlop.markista.doclet.Configuration;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ClassNode;
import io.github.sandydunlop.markista.model.PackageMember;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.Reference;


/// This class woks calculates the paths for Markdown documents 
/// to link between different packages and to URLs of external
/// packages and their contents.
public class LinkResolver {
    private static final List<String> primitives = Arrays.asList("boolean","byte","char","short","int","long","float","double");
    private static String location = null;
    private static HashMap<String,String> nativePackageNames = new HashMap<>();
    private static HashMap<String,String> suffix = new HashMap<>();
    private static final ModuleLayer moduleLayer = ModuleLayer.boot();
    private static Api api = null;
    private static String squashedDirectories = null;

    private LinkResolver() {
        // This hides the public constructor
    }

    public static void setApi(Api a) {
        api = a;
    }

    public static void setSquashedDirectories(String sd) {
        squashedDirectories = sd;
    }

    public static void setLocation(String loc) {
        location = loc;
    }

    public static void addNativeModule(String moduleName, String baseUrl, String s) {
        Optional<Module> module = moduleLayer.findModule(moduleName);
        if (module.isPresent()) {
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
        }
        String className = id;
        if (dot > 0 && dot < id.length()) {
            return className.substring(dot);
        }
        return "";
    }

    public static Reference resolve(String to) {
        return resolve(location, to);
    }

    public static Reference resolve(String from, String to) {
        String original = to;
        Reference link = new Reference();
        if (to == null) return link;
        if (to.equals("?") || to.indexOf("<") > -1) {
            return link;
        }
        if ("void".equals(to) || "Void".equals(to)){
            link.setKind(Reference.Kind.VOID);
            link.setScope(Reference.Scope.NATIVE);
            return link;
        }
        if (primitives.contains(to)){
            link.setKind(Reference.Kind.PRIMITIVE);
            link.setScope(Reference.Scope.NATIVE);
            return link;
        }
        String url = resolveNative(to);
        if (url != null) {
            link.setUri(url);
            link.setKind(Reference.Kind.URL);
            link.setScope(Reference.Scope.NATIVE);
            return link;
        }
        String fromPackageName = getPackageName(from);
        String toPackageName = getPackageName(to);
        String toClassName = getClassName(to);
        if (toPackageName.isEmpty()) {
            to = qualifyClass(fromPackageName, toClassName);
            toPackageName = getPackageName(to);
            toClassName = getClassName(to);
        }
        if (!isPackageQualified(fromPackageName, toPackageName)) {
            to = qualifyPackage(fromPackageName, toPackageName);
            toPackageName = getPackageName(to);
            toClassName = getClassName(to);
        }
        if (toPackageName.isEmpty()) {
            Configuration.getReporter().print(Diagnostic.Kind.WARNING, "Reference not found: " + original);
            link.setKind(Reference.Kind.NONE);
            return link;
        }
        link.setUri(relativize(fromPackageName, toPackageName));
        link.setKind(Reference.Kind.PACKAGE);
        link.setScope(Reference.Scope.LOCAL);
        if (!toClassName.isEmpty()) {
            if (!link.getUri().isEmpty()) link.setUri(link.getUri() + "/");
            link.setUri(link.getUri() + toClassName);
            link.setKind(Reference.Kind.TYPE);
        }
        return link;
    }

    public static String resolveNative(String to) {
        int dot = 0;
        do {
            dot = to.indexOf(".", dot + 1);
            if (dot != -1) {
                String id = to.substring(0, dot);
                String baseUrl = nativePackageNames.get(id);
                if (baseUrl != null) {
                    String toPackage = getPackageName(to);
                    String toClass = getClassName(to);
                    return baseUrl + "/" + toPackage.replace(".", "/") + "/" + toClass+ suffix.get(id);
                }
            }
        } while(dot != -1);
        return null;
    }

    public static boolean isPackageQualified(String from, String to) {
        return to.indexOf('.') > -1;
    }

    public static String qualifyClass(String from, String to) {
        // 'from' could be used to ensure the closest matching class is chosen
        for (PackageMember member : api.getClasses()) {
            if (member instanceof ClassNode classNode) {
                if (classNode.getSimpleName().equals(to)) {
                    return classNode.getQualifiedName();
                }
            }
        }
        return "";
    }

    public static String qualifyPackage(String from, String to) {
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
        return relativize(location, to);
    }

    public static String relativize(String from, String to) {
        if (from == null || to == null) return null;
        if (squashedDirectories != null && !squashedDirectories.isEmpty()) {
            if (from.startsWith(squashedDirectories)) {
                from = from.substring(squashedDirectories.length() + 1);
            }
            if (to.startsWith(squashedDirectories)) {
                to = to.substring(squashedDirectories.length() + 1);
            }
        }
        String[] fromParts = from.split("\\.");
        String[] toParts = to.split("\\.");
        StringBuilder rel = new StringBuilder();
        int p = 0;
        if (!from.isEmpty()) {
            for (p=fromParts.length-1; p>=0; p--) {
                if (p<toParts.length) {
                    if (fromParts[p].equals(toParts[p])) {
                        p++;
                        break;
                    }
                }
                if (!rel.isEmpty()) rel.append("/");
                rel.append("..");
            }
        }
        if (p == -1) p = 0;
        for (;p < toParts.length; p++) {
            if (!rel.isEmpty()) rel.append("/");
            rel.append(toParts[p]);
        }
        return rel.toString();
    }
}
