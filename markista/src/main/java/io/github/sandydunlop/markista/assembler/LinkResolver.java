package io.github.sandydunlop.markista.assembler;

import io.github.sandydunlop.markista.core.Configuration;
import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.Reference;
import io.github.sandydunlop.markista.model.Reference.Kind;
import io.github.sandydunlop.markista.model.Reference.Scope;
import io.github.sandydunlop.markista.model.TypeNode;
import io.github.sandydunlop.markista.model.TypeView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

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
    private static final String DOT_CLASS = ".class";
    /// The Context singleton instance providing access to the current documentation generation context,
    /// including configuration, current module/package/type names, and reporting utilities.
    static Context ctx;
    private static final String DOT_HTML = ".html";
    private static final String JAVA_24_URL = "https://docs.oracle.com/en/java/javase/24/docs/api/";
    private static final List<String> primitives = Arrays.asList("boolean","byte","char","short","int","long","float","double");
    private static final ModuleLayer moduleLayer = ModuleLayer.boot();
    private static HashMap<String,String> nativeModuleNames = new HashMap<>();
    private static HashMap<String,String> nativePackageNames = new HashMap<>();
    private static HashMap<String,String> suffix = new HashMap<>();

    static List<String> siblingModules = new ArrayList<>();
    static Map<String, String> classToModule = new HashMap<>();
    static String siblingModuleName = "";
    static Set<String> siblingClassNames;

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
    /// @param c Context for all documentation operations
    public static void init(Api a, Context c) {
        api = a;
        nativeModuleNames = new HashMap<>();
        nativePackageNames = new HashMap<>();
        suffix = new HashMap<>();
        ctx = c;
        configureSiblingModules();
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
        if (dot > 0 && dot < id.length()) {
            return id.substring(0, dot - 1);
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
        if (dot > 0 && dot < id.length()) {
            return id.substring(dot);
        }
        return "";
    }

    /// Resolves a link reference. The supplied `link` parameter must specify the
    /// target of the link: a class, a module, etc. If the link can be resolved, 
    /// the details of the type of link are set in the `link` object before it is
    /// returned. 
    /// @param link A [Reference] object specifying the target of the link
    /// @return The `link` with its `uri` field set, or its `kind` field set to `UNKNOWN`
    /// if the link was not able to be resolved.
    public static Reference resolve(Reference link) {
        if (link == null || link.getTarget() == null || link.getTarget().isEmpty()) {
            return link;
        }

        link.setResolved(false);
        if (link.getOrigin() == null && ctx != null) {
            link.setOrigin(ctx.getPackageName());
        }

        if (link.getTarget().contains("://")) {
            link.setUri(link.getTarget());
            link.setKind(Reference.Kind.URL);
            link.setResolved(true);
            return link;
        }

        resolveUnsupported(link);
        if (link.getKind() == Reference.Kind.UNSUPPORTED) {
            return link;
        }

        if (link.getTarget().endsWith("/")) {
            link.setKind(Reference.Kind.MODULE);
        }

        if (resolvePrimitiveOrVoid(link)) return link;
        if (resolveNativePackageOrType(link)) return link;
        if (resolveLocalPackageOrType(link)) return link;
        if (resolveLocalModule(link)) return link;
        if (resolveSiblingModule(link)) return link;
        if (resolveSiblingType(link)) return link;

        if (!link.isResolved()) {
            link.setKind(Reference.Kind.UNKNOWN);
        }
        return link;
    }

    /// Checks if the target is unsupported by the `LinkResolver`.
    /// @param link The target to be resolved.
    /// @return A `Reference` with `Reference.Kind.UNKNOWN` if unsupported, else `Reference.Kind.NONE`.
    static Reference resolveUnsupported(Reference link) {
        if (link.getTarget().equals("?") || link.getTarget().contains("<")) {
            link.setScope(Scope.UNKNOWN);
            link.setKind(Kind.UNSUPPORTED);
        }
        return link;
    }

    /// Checks if the target is a primitive type or void.
    /// @param link The target to be resolved.
    /// @return True if the reference resolved to primitive or void, else false.
    static boolean resolvePrimitiveOrVoid(Reference link) {
        if (link.getKind() == Kind.UNKNOWN || link.getKind() == Kind.PRIMITIVE || link.getKind() == Kind.VOID) {
            if ("void".equals(link.getTarget()) || "Void".equals(link.getTarget())){
                link.setScope(Scope.NATIVE);
                link.setKind(Kind.VOID);
                link.setResolved(true);
                return true;
            }
            if (primitives.contains(link.getTarget())){
                link.setScope(Scope.NATIVE);
                link.setKind(Kind.PRIMITIVE);
                link.setResolved(true);
                return true;
            }
        }
        return false;
    }

    /// Returns canonical package and class names for a given type name, ensuring both parts are qualified.
    /// @param name The type name to qualify.
    /// @return A String array: {packageName, className}.
    public static String[] qualifyType(String name) {
        String toPackageName = getPackageName(name);
        String toClassName = getClassName(name);
        if (toPackageName.isEmpty()) {
            String qualifiedTo = "";
            for (TypeView member : api.getTypes()) {
                if (member instanceof TypeNode classNode && classNode.getSimpleName().equals(toClassName)) {
                    qualifiedTo = classNode.getQualifiedName();
                    break;
                }
            }
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
    /// @param link The link to be resolved
    /// @return True if the reference resolved to a native package or type.
    static boolean resolveNativePackageOrType(Reference link) {
        if (link.getKind() == Kind.UNKNOWN || link.getKind() == Kind.PACKAGE || link.getKind() == Kind.TYPE) {
            String[] qualified = qualifyType(link.getTarget());
            String toPackageName = qualified[0];
            String toClassName = qualified[1];
            if (toPackageName.isEmpty()) {
                return false;
            } else {
                return resolveNativePackageTypeInternal(link, toPackageName, toClassName);
            }
        }
        return false;
    }

    private static boolean resolveNativePackageTypeInternal(Reference link, String toPackageName, String toClassName) {
        String baseUrl = nativePackageNames.get(toPackageName);
        if (baseUrl != null) {
            link.setScope(Reference.Scope.NATIVE);
            link.setKind(Reference.Kind.URL);
            String uri = baseUrl + "/" + toPackageName.replace(".", "/");
            if (!toClassName.isEmpty()) {
                if (!link.getUri().isEmpty()) link.setUri(link.getUri() + "/");
                uri = uri + "/" + toClassName;
                if (link.getLabel() == null || link.getLabel().isEmpty()) {
                    link.setLabel(toPackageName + "." + toClassName);
                }
                link.setClassName(toPackageName + "." + toClassName);
            } else {
                link.setLabel(toPackageName);
            }
            uri += suffix.get(toPackageName);
            link.setUri(uri);
            link.setResolved(true);
            return true;
        }
        return false;
    }

    /// Resolves a local package or type within the documented API to a relative link.
    /// @param link the link to resolve
    /// @return True if the reference resolved to a local package or type.
    static boolean resolveLocalPackageOrType(Reference link) {
        if (link.getKind() == Kind.UNKNOWN || link.getKind() == Kind.PACKAGE || link.getKind() == Kind.TYPE) {
            String[] qualified = qualifyType(link.getTarget());
            String toPackageName = qualified[0];
            String toClassName = qualified[1];
            if (toPackageName.isEmpty()) {
                return false;
            } else {
                return resolveLocalPackageTypeInternal(link, toPackageName, toClassName);
            }
        }
        return false;
    }

    static boolean resolveLocalPackageTypeInternal(Reference link, String toPackageName, String toClassName) {
        String fromPackageName = getPackageName(link.getOrigin());
        if (getPackageName(toPackageName).isEmpty()) {
            return false;
        }

        if (!toClassName.isEmpty()) {
            String qualifiedClassName = toPackageName + "." + toClassName;
            TypeNode typeNode = api.getTypeNode(qualifiedClassName);
            if (typeNode == null) {
                return false;
            }
        }
        
        PackageNode packageNode = api.getPackageNode(toPackageName);
        if (packageNode == null) return false;
        link.setScope(Reference.Scope.LOCAL);
        link.setKind(Reference.Kind.PACKAGE);
        link.setResolved(true);
        link.setUri(relativizeWithModules(fromPackageName, toPackageName));
        if (!toClassName.isEmpty()) {
            if (link.getLabel() == null || link.getLabel().isEmpty()) {
                link.setLabel(toPackageName + "." + toClassName);
            }
            link.setClassName(toPackageName + "." + toClassName);
            addClassToReference(toClassName, link);
        } else {
            link.setLabel(toPackageName);
        }
        return true;
    }

    /// Appends the class name to the URI on the Reference and sets kind TYPE if not URL.
    /// @param className The class name to add.
    /// @param link The Reference object to modify.
    static void addClassToReference(String className, Reference link) {
        if (!link.getUri().isEmpty()) link.setUri(link.getUri() + "/");
        link.setUri(link.getUri() + className);
        if (link.getKind() != Reference.Kind.URL) {
            link.setKind(Reference.Kind.TYPE);
        }
    }

    /// Resolves a module reference by name to a Reference either local or native.
    /// @param link the link to be resolved
    /// @return True if the reference resolved to a local module.
    static boolean resolveLocalModule(Reference link) {
        if (link.getKind() == Kind.UNKNOWN || link.getKind() == Kind.MODULE) {
            String target = link.getTarget();
            if (target.endsWith("/")) {
                target = target.substring(0, target.length() - 1);
            }
            String apiRoot = relativize(link.getOrigin(), "");
            ModuleNode moduleNode = getModule(target);
            Path path = Path.of(apiRoot, "..", link.getTarget());
            if (moduleNode != null) {
                link.setScope(Scope.LOCAL);
                link.setKind(Kind.MODULE);
                link.setUri(path.toString());
                link.setResolved(true);
                return true;
            }
            String baseUrl = nativeModuleNames.get(target);
            if (baseUrl != null) {
                link.setScope(Scope.NATIVE);
                link.setKind(Kind.URL);
                link.setUri(baseUrl + "/module-summary.html");
                link.setResolved(true);
                return true;
            }
        }
        return false;
    }

    /// Attempts to resolve the link as a sibling module relative to the current context.
    /// @param link The Reference object to resolve.
    /// @return True if the reference resolved to a sibling module.
    static boolean resolveSiblingModule(Reference link) {
        if ((link.getKind() == Kind.UNKNOWN || link.getKind() == Kind.MODULE) && 
                siblingModules.contains(link.getTarget())) {
            String toRoot = relativize(link.getOrigin(), "");
            Path path = Path.of(toRoot, "..", link.getTarget());
            link.setUri(path.toString());
            link.setScope(Scope.SIBLING);
            link.setKind(Kind.MODULE);
            link.setResolved(true);
            return true;
        }
        return false;
    }

    /// Attempts to resolve the link as a sibling type relative to the current context.
    /// @param link The Reference object to resolve.
    /// @return True if the reference resolved to a sibling type.
    static boolean resolveSiblingType(Reference link) {
        if (link.getKind() == Kind.UNKNOWN || link.getKind() == Kind.TYPE) {
            String moduleName = classToModule.get(link.getTarget());
            if (moduleName != null) {
                String qualifiedClassName = link.getTarget();
                String path = relativizeWithSiblingModule(link.getOrigin(), qualifiedClassName, moduleName);
                link.setUri(path);
                link.setScope(Scope.SIBLING);
                link.setKind(Kind.TYPE);
                link.setResolved(true);
                return true;
            }
        }
        return false;
    }

    /// Returns the ModuleNode for the named module in the current API.
    /// @param moduleName The module's name.
    /// @return The ModuleNode if found, else null.
    static ModuleNode getModule(String moduleName) {
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
    static boolean isPackageQualified(String name) {
        return name.indexOf('.') > -1;
    }

    /// Qualifies a simple package name to its fully qualified name within the API, if exists.
    /// @param simpleName The package simple name.
    /// @return The fully qualified package name or empty string if not found.
    public static String qualifyPackage(String simpleName) {
        for (PackageNode node : api.getPackages()) {
            int p = node.getQualifiedName().lastIndexOf(".");
            if (node.getQualifiedName().substring(p + 1).equals(simpleName)) {
                return node.getQualifiedName();
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
    static String relativizeWithModules(String from, String to) {
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
            return relativize(from, to);
        } else {
            return Path.of(relativize(from, ""), toModuleName, relativize("", to)).toString();
        }
    }

    /// Computes a relative path from the current package context, considering sibling modules.
    /// @param from The source package name.
    /// @param to The target package name.
    /// @return The relative path string including sibling module base if applicable.
    static String relativizeWithSiblingModule(String from, String to, String toModule) {
        String toRoot = relativize(from, "");
        String toTarget = relativize("", to);
        Path path = Path.of(toRoot, "..", toModule, toTarget);
        return path.toString();
    }

    /// Produces a relative path string from one package to another by splitting and comparing components.
    /// Supports flattened directories if set.
    /// @param from The source package name.
    /// @param to The target package name.
    /// @return The relative path string.
    public static String relativize(String from, String to) {
        if (from == null || to == null) return "";
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
    static String flattenDirectory(String path) {
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
    static int findCommonIndex(String[] fromParts, String[] toParts) {
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

    /// Appends parent directory segments `..` to the relative path string builder.
    /// @param rel The StringBuilder accumulating the path.
    /// @param count The number of parent directory segments to append.
    static void appendParentDirs(StringBuilder rel, int count) {
        for (int i = 0; i < count; i++) {
            if (!rel.isEmpty()) rel.append("/");
            rel.append("..");
        }
    }

    /// Appends target directory segments to the relative path string builder starting at index start.
    /// @param rel The StringBuilder accumulating the path.
    /// @param toParts Array of target path segments.
    /// @param start The start index for appending segments.
    static void appendTargetDirs(StringBuilder rel, String[] toParts, int start) {
        for (int i = start; i < toParts.length; i++) {
            if (toParts[i].isEmpty()) continue;
            if (!rel.isEmpty()) rel.append("/");
            rel.append(toParts[i]);
        }
    }

    /// Configures the known sibling modules for link resolution, typically loading their info.
    /// This method updates internal structures to recognize sibling modules for proper linking.
    static void configureSiblingModules() {
        List<String> linkExternalList = List.of();
        if (Configuration.getLinkExternal() != null) {
            String[] modules = Configuration.getLinkExternal().split(":");
            linkExternalList = Arrays.asList(modules);
        }

        List<String> modulePathList = List.of();
        if (Configuration.getModulePaths() != null) {
            String[] pathList = Configuration.getModulePaths().split(":");
            modulePathList = Arrays.asList(pathList);
        }

        classToModule = new HashMap<>();
        for (String modulePathString : modulePathList) {
            File file = Paths.get(modulePathString).toFile();
            siblingModuleName = "";
            siblingClassNames = new java.util.HashSet<>();
            if (file.isDirectory()) {
                processDirectory(file);
            } else if (file.toString().toLowerCase().endsWith(".jar")) {
                try (JarFile jarFile = new JarFile(file)) {
                    processJarFile(jarFile);
                } catch (IOException _) {
                    ctx.reportError("Error reading JAR file: " + file);
                }
            } 
            siblingModules.add(siblingModuleName);
            if (linkExternalList.contains(siblingModuleName)) {
                for (String className : siblingClassNames) {
                    classToModule.put(className, siblingModuleName);
                }
            }
        }
    }

    static void processDirectory(File directory) {
        try {
            URL url = URL.of(directory.toURI(), null);
            URL[] urls = new URL[] {url};
            processDirectoryUrl(directory, urls);  
        }catch(MalformedURLException _) {
            // Nothing to do here
        }
    }

    static void processDirectoryUrl(File directory, URL[] urls) {
        try (URLClassLoader classLoader = new URLClassLoader(urls)) {
            try(Stream<Path> classFiles = Files.walk(directory.toPath())) {
                for (Path classFile : classFiles.toList()) {
                    if (classFile.toString().endsWith(DOT_CLASS)) {
                        processClassFile(directory, classFile.toFile(), classLoader);
                    }
                }
            }
        } catch (IOException _) {
            ctx.reportError("Error reading classes in directory: " + directory.getAbsolutePath());
        }
    }

    static void processClassFile(File directory, File file, URLClassLoader classLoader) throws IOException{
        try {
            String relativePath = directory.toURI().relativize(file.toURI()).getPath();
            String className = relativePath.replace(File.separatorChar, '.').replace(DOT_CLASS, "");
            if (className.equals("module-info")) {
                InputStream is = new FileInputStream(file);
                ModuleDescriptor descriptor = ModuleDescriptor.read(is);
                siblingModuleName = descriptor.name();
            } else {
                Class<?> clazz = classLoader.loadClass(className);
                siblingClassNames.add(clazz.getName());
                for (Class<?> declaredClass : clazz.getDeclaredClasses()) {
                    siblingClassNames.add(declaredClass.getName());
                }
            }
        } catch (Exception _) {
            ctx.reportError("Failed ot read class file: " + file);
        }
    }
    
    // SonarQube thinks this is extracting the JAR file.
    // It's only reading the list of contents and extracting enough to get the module descriptor.
    /// Processes a JAR file to extract module and package information relevant for sibling module linking.
    /// @param jarFile The file path to the JAR file.
    /// @throws IOException if an error occurs while processing the JAR.
    @java.lang.SuppressWarnings("squid:S5042")
    static void processJarFile(JarFile jarFile) throws IOException{
        siblingModuleName = "Unnamed Module";
        // Iterate over all entries in the JAR
        for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements();) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();

            // Process module-info.class
            if (entryName.equals("module-info.class")) {
                InputStream is = jarFile.getInputStream(entry);
                ModuleDescriptor descriptor = ModuleDescriptor.read(is);
                siblingModuleName = descriptor.name();
            }

            // Process other class files
            else if (entryName.endsWith(DOT_CLASS)) {
                String className = entryName
                    .replace("/", ".")
                    .replace("\\", ".")
                    .substring(0, entryName.length() - 6); // Remove DOT_CLASS
                siblingClassNames.add(className);
            }
        }
    }

    /// Adds a native Java module URL for linking purposes using a standard Oracle Javadoc base URL.
    /// @param moduleName The native module name (e.g., java.base).
    static void addNativeModule(String moduleName) {
        // Tell the link resolver what web address to find docs for certain Java modules at
        LinkResolver.addNativeModuleUrl(moduleName, JAVA_24_URL + moduleName, DOT_HTML);
    }

    /// Adds known native modules for Java SE 24 to the resolver.
    /// This populates internal mappings for native module and package documentation URLs.
    @SuppressWarnings("SpellCheckingInspection")
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