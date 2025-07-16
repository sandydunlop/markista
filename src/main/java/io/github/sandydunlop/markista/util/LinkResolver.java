package io.github.sandydunlop.markista.util;

import java.util.HashMap;
import java.util.Optional;

import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ClassNode;
import io.github.sandydunlop.markista.model.PackageNode;


/// This class woks calculates the paths for Markdown documents 
/// to link between different packages and to URLs of external
/// packages and their contents.
public class LinkResolver {
    private static HashMap<String,String> nativePackageNames = new HashMap<>();
    private static HashMap<String,String> suffix = new HashMap<>();
    private static final ModuleLayer moduleLayer = ModuleLayer.boot();
    private static Api api = null;

    private LinkResolver() {
        // This hides the public constructor
    }

    public static void setApi(Api a) {
        api = a;
    }

    public static void addLocalPackage(String identifier) {
        // localPackageNames.add(identifier);
    }


    public static void addNativeModule(String moduleName, String baseUrl, String s) {
        Optional<Module> module = moduleLayer.findModule(moduleName);
        if (module.isPresent()) {
            for (String packageName : module.get().getPackages()) {
                nativePackageNames.put(packageName, baseUrl);
                suffix.put(packageName, s);
            }
        } else {
            System.err.println("Module not found: " + moduleName);
        }
    }

    private static String getPackageName(String id) {
        if (id == null || id.isEmpty() || Character.isUpperCase(id.charAt(0))) {
            return "";
        }
        int dot;
        for (dot=0; dot<id.length() && !Character.isUpperCase(id.charAt(dot)); dot++) {
        }
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

    public static Link resolve(String from, String to) {
        Link link = new Link();
        if (to == null) return link;
        if (to.indexOf("<") > -1) return link;
        String url = resolveExternal(to);
        if (url != null) {
            link.path = url;
            link.type = Type.EXTERNAL;
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
        if (!isQualified(fromPackageName, toPackageName)) {
            to = qualifyPackage(fromPackageName, toPackageName);
            toPackageName = getPackageName(to);
            toClassName = getClassName(to);
        }
        if (toPackageName.isEmpty()) return link;
        link.path =  relativize(fromPackageName, toPackageName);
        link.type = Type.PACKAGE;
        if (!toClassName.isEmpty()) {
            if (!link.path.isEmpty()) link.path += "/";
            link.path += toClassName;
            link.type = Type.CLASS;
        }
        return link;
    }

    public static String resolveExternal(String to) {
        int dot = 0;
        do {
            dot = to.indexOf(".", dot + 1);
            if (dot != -1) {
                String id = to.substring(0, dot);
                String baseUrl = nativePackageNames.get(id);
                if (baseUrl != null) {
                    String link = baseUrl + "/" + to.replace(".", "/") + suffix.get(id);
                    return link;
                }
            }
        } while(dot != -1);
        return null;
    }

    public static boolean isQualified(String from, String to) {
        int p = from.indexOf('.');
        String first = from.substring(0, p);
        return to.length() > p && to.substring(0, p).equals(first);
    }

    public static String qualifyClass(String from, String to) {
        // 'from' could be used to ensure the closest matching class is chosen
        for (ClassNode node : api.getClasses()) {
            if (node.simpleName.equals(to)) {
                return node.qualifiedName;
            }
        }
        return "";
    }

    public static String qualifyPackage(String from, String to) {
        // 'from' could be used to ensure the closest matching package is chosen
        for (PackageNode node : api.getPackages()) {
            int p = node.qualifiedName.lastIndexOf(".");
            if (node.qualifiedName.substring(p + 1).equals(to)) {
                return node.qualifiedName;
            }
        }
        return "";
    }

    public static String relativize(String from, String to) {
        if (from == null || to == null) return null;
        String[] fromParts = from.split("\\.");
        String[] toParts = to.split("\\.");
        StringBuilder rel = new StringBuilder();
        int p;
        for (p=fromParts.length-1; p>0; p--) {
            if (p<toParts.length) {
                if (fromParts[p].equals(toParts[p])) {
                    p++;
                    break;
                }
            }
            if (rel.length() > 0) rel.append("/");
            rel.append("..");
        }
        for (;p < toParts.length; p++) {
            if (!rel.isEmpty()) rel.append("/");
            rel.append(toParts[p]);
        }
        return rel.toString();
    }

    public enum Type {
        PACKAGE,
        CLASS,
        EXTERNAL,
        NOTHING
    }

    public static class Link {
        public Type type = Type.NOTHING;
        public String path = null;
    }
}
