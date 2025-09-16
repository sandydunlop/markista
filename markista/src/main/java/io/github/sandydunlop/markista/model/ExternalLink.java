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

    URI uri;
    List<String> modules = new ArrayList<>();
    List<String> packages = new ArrayList<>();
    Map<String,String> packageToModule = new HashMap<>();

    public ExternalLink(Context c, URI u) {
        ctx = c;
        uri = u;
        api = new Api("");
    }

    public URI getUri() {
        return uri;
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

    public boolean isWebLink() {
        return uri.getScheme() != null && uri.getScheme().startsWith("http");
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
                PackageReference packageRef = new PackageReference(line);
                if (moduleNode != null) {
                    moduleNode.addPackage(packageRef);
                }
                PackageNode packageNode = new PackageNode(line);
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
        URI fileUri;
        if (uri.getScheme() == null || uri.getScheme().equals("file")) {
            Path directory = Path.of(uri.toString());
            Path p = Path.of(ctx.getOutputDirectory(), directory.toString(), fileName);
            String absolute = "file://" + p.toAbsolutePath().normalize().toString();
            fileUri = URI.create(absolute);
        } else {
            fileUri = URI.create(uri + "/" + fileName);
        }
        return fileUri;
    }

    private List<String> loadFileFromUri(URI uri) {
        List<String> content = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(uri.toURL().openStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.add(line);
            }
        } catch (Exception _) {
            ctx.reportError("Failed to read from URI: " + uri);
        }
        return content;
    }
}
