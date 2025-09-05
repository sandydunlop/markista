package io.github.sandydunlop.markista.model;

import io.github.sandydunlop.markista.core.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExternalLink {
    Context ctx;
    Api api;

    String path = "";
    List<String> modules = new ArrayList<>();
    List<String> packages = new ArrayList<>();
    Map<String,String> packageToModule = new HashMap<>();

    boolean isWebLink = false;

    public ExternalLink(Context c, String p) {
        ctx = c;
        path = p;
        api = new Api("");
    }

    public String getPath() {
        return path;
    }

    public List<String> getModules() {
        return modules;
    }

    public List<String> getPackages() {
        return packages;
    }

    public Map<String,String> getPackageToModule() {
        return packageToModule;
    }

    public void setWebLink(boolean b) {
        isWebLink = b;
    }

    public boolean isWebLink() {
        return isWebLink;
    }

    public Api getApi() {
        return api;
    }

    public void load() {
        String moduleName = "";
        ModuleNode moduleNode = null;
        URI uri = uriForFile("element-list");
        List<String> content = loadFileFromUri(uri);
        for (String line : content) {
            if (line.startsWith("module:")) {
                moduleName = line.substring(7);
                moduleNode = new ModuleNode(moduleName);
                api.addModule(moduleNode);
            } else {
                PackageNode packageNode = new PackageNode(line);
                if (moduleNode != null) {
                    moduleNode.addPackage(packageNode);
                }
                api.addPackage(packageNode);
                packages.add(line);
                if (!moduleName.isEmpty()) {
                    packageToModule.put(line, moduleName);
                } else {
                    // Unnamed Module
                }
            }
        }
    }

    private URI uriForFile(String fileName) {
        URI uri = URI.create(path);
        if (uri.getScheme() == null || uri.getScheme().equals("file")) {
            Path p = Path.of(ctx.getOutputDirectory(), path, fileName);
            String absolute = "file://" + p.toAbsolutePath().normalize().toString();
            uri = URI.create(absolute);
            isWebLink = false;
        } else {
            uri = URI.create(path + "/" + fileName);
            isWebLink = true;
        }
        return uri;
    }

    private List<String> loadFileFromUri(URI uri) {
        List<String> content = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(uri.toURL().openStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.add(line);
            }
        } catch (Exception e) {
            ctx.reportError("Failed to read from URI: " + uri);
        }
        return content;
    }
}
