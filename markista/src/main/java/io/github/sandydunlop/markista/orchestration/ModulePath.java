package io.github.sandydunlop.markista.orchestration;

import io.github.sandydunlop.markista.core.Configuration;
import io.github.sandydunlop.markista.core.Context;

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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class ModulePath {
    private static final String DOT_CLASS = ".class";
    Context ctx;
    Set<String> moduleNames = new java.util.HashSet<>();
    Set<String> classNames = new java.util.HashSet<>();
    Map<String, String> classToModule = new HashMap<>();

    protected ModulePath() {
        // Nothing to see here
    }

    public ModulePath(Context c, String modulePath) {
        ctx = c;
        loadModulePath(modulePath);
    }

    public Set<String> getModules() {
        return moduleNames;
    }

    public String getModuleForClass(String className) {
        return classToModule.get(className);
    }

    public boolean hasModule(String moduleName) {
        return moduleNames.contains(moduleName);
    }

    public boolean hasClass(String className) {
        return classNames.contains(className);
    }

    void loadModulePath(String modulePath) {
        moduleNames = new java.util.HashSet<>();
        classToModule = new HashMap<>();
        List<String> modulePathList = List.of();
        if (Configuration.getModulePaths() != null) {
            String[] pathList = modulePath.split(":");
            modulePathList = Arrays.asList(pathList);
        }

        classToModule = new HashMap<>();
        for (String modulePathString : modulePathList) {
            File file = Paths.get(modulePathString).toFile();
            String moduleName = "";
            classNames = new java.util.HashSet<>();
            if (file.isDirectory()) {
                moduleName = processDirectory(file, moduleName);
            } else if (file.toString().toLowerCase().endsWith(".jar")) {
                try (JarFile jarFile = new JarFile(file)) {
                    moduleName = processJarFile(jarFile, moduleName);
                } catch (IOException _) {
                    ctx.reportError("Error reading JAR file: " + file);
                }
            }
            moduleNames.add(moduleName);
        }
    }

    String processDirectory(File directory, String moduleName) {
        try {
            URL url = URL.of(directory.toURI(), null);
            URL[] urls = new URL[] {url};
            processDirectoryUrl(directory, urls, moduleName);
        }catch(MalformedURLException _) {
            // Nothing to do here
        }
        return moduleName;
    }

    String processDirectoryUrl(File directory, URL[] urls, String moduleName) {
        try (URLClassLoader classLoader = new URLClassLoader(urls)) {
            try(Stream<Path> classFiles = Files.walk(directory.toPath())) {
                for (Path classFile : classFiles.toList()) {
                    if (classFile.toString().endsWith(DOT_CLASS)) {
                        moduleName = processClassFile(directory, classFile.toFile(), classLoader, moduleName);
                    }
                }
            }
        } catch (IOException _) {
            ctx.reportError("Error reading classes in directory: " + directory.getAbsolutePath());
        }
        return moduleName;
    }

    String processClassFile(File directory, File file, URLClassLoader classLoader, String moduleName) throws IOException{
        String className = "";
        String relativePath = "";
        try {
            relativePath = directory.toURI().relativize(file.toURI()).getPath();
            className = relativePath.replace(File.separatorChar, '.').replace(DOT_CLASS, "");
            if (className.equals("module-info")) {
                InputStream is = new FileInputStream(file);
                ModuleDescriptor descriptor = ModuleDescriptor.read(is);
                moduleName = descriptor.name();
            } else {
                Class<?> clazz = classLoader.loadClass(className);
                classNames.add(clazz.getName());
                for (Class<?> declaredClass : clazz.getDeclaredClasses()) {
                    classNames.add(declaredClass.getName());
                }
            }
        } catch (java.lang.NoClassDefFoundError e) {
            ctx.reportError("Class not found: " + className + "\n" + e.getMessage());
        } catch (Exception _) {
            ctx.reportError("Failed to read class file: " + file);
        }
        return moduleName;
    }

    // SonarQube thinks this is extracting the JAR file.
    // It's only reading the list of contents and extracting enough to get the module descriptor.
    /// Processes a JAR file to extract module and package information relevant for sibling module linking.
    /// @param jarFile The file path to the JAR file.
    /// @throws IOException if an error occurs while processing the JAR.
    @java.lang.SuppressWarnings("squid:S5042")
    String processJarFile(JarFile jarFile, String moduleName) throws IOException{
        String newModuleName = "Unnamed Module";
        // Iterate over all entries in the JAR
        for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements();) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();

            // Process module-info.class
            if (entryName.equals("module-info.class")) {
                InputStream is = jarFile.getInputStream(entry);
                ModuleDescriptor descriptor = ModuleDescriptor.read(is);
                newModuleName = descriptor.name();
            }

            // Process other class files
            else if (entryName.endsWith(DOT_CLASS)) {
                String className = entryName
                    .replace("/", ".")
                    .replace("\\", ".")
                    .substring(0, entryName.length() - 6); // Remove DOT_CLASS
                classNames.add(className);
            }
        }
        return newModuleName;
    }
}
