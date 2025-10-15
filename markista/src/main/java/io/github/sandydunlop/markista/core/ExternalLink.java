package io.github.sandydunlop.markista.core;

import io.github.sandydunlop.cascara.model.SemanticModel;
import io.github.sandydunlop.cascara.model.ModuleNode;
import io.github.sandydunlop.cascara.model.PackageNode;
import io.github.sandydunlop.cascara.model.PackageReference;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExternalLink {
    SemanticModel api;

    URI uri;
    String outputDirectory;
    List<String> modules = new ArrayList<>();
    List<String> packages = new ArrayList<>();
    Map<String,String> packageToModule = new HashMap<>();

    public ExternalLink(URI u, String d) {
        uri = u;
        api = new SemanticModel("");
        outputDirectory = d;
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

    public SemanticModel getSemanticModel() {
        return api;
    }

    public void load() throws IOException {
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
            Path p = Path.of(outputDirectory, directory.toString(), fileName);
            String absolute = "file://" + p.toAbsolutePath().normalize().toString();
            fileUri = URI.create(absolute);
        } else {
            fileUri = URI.create(uri + "/" + fileName);
        }
        return fileUri;
    }

    private List<String> loadFileFromUri(URI uri) throws IOException {
        List<String> content = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(uri.toURL().openStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.add(line);
            }
        } catch (Exception e) {
            throw new IOException("Failed to read from URI: " + uri);
        }
        return content;
    }

}
