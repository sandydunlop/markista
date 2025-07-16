package io.github.sandydunlop.markista.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;

/// This class woks calculates the paths for Markdown documents 
/// to link between different packages and to URLs of external
/// packages and their contents.
public class LinkResolver {
    private static HashSet<String> localPackageNames = new HashSet<>();
    private static HashMap<String,String> nativePackageNames = new HashMap<>();
    private static HashMap<String,String> suffix = new HashMap<>();
    private static final ModuleLayer moduleLayer = ModuleLayer.boot();

    private LinkResolver() {
        // This hides the public constructor
    }

    public static void addLocalPackage(String identifier) {
        localPackageNames.add(identifier);
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

    public static String resolve(String from, String to) {
        if (to == null) return null;
        if (to.indexOf("<") > -1) return null;
        String fromPackageName = getPackageName(from);
        String toPackageName = getPackageName(to);
        String toClassName = getClassName(to);
        int dot = toPackageName.lastIndexOf(".");
        if (dot > -1) {
            if (localPackageNames.contains(toPackageName)) {
                fromPackageName += ".";
                toPackageName += ".";
                dot = 0;
                do {
                    int nextDotFrom = fromPackageName.indexOf(".", dot + 1);
                    int nextDotTo = toPackageName.indexOf(".", dot + 1);
                    if (nextDotFrom == -1 || nextDotTo == -1 || 
                            !fromPackageName.substring(0, nextDotFrom).equals(toPackageName.substring(0, nextDotTo))) {
                        int dotsRemaining = (int) fromPackageName.substring(dot).chars().filter(ch -> ch == '.').count();
                        String pathRemaining = toPackageName.substring(dot + 1);
                        if (dotsRemaining < 2) {
                            return toClassName;
                        }else{
                            String link = "../".repeat(dotsRemaining - 1) + pathRemaining.replace(".","/");
                            return link + "/" + toClassName;
                        }
                    }
                    dot = nextDotFrom;
                } while(true);
            }
            dot = 0;
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
        }
        if (toPackageName.isEmpty()) {
            return toClassName;
        }
        return null;
    }
}
