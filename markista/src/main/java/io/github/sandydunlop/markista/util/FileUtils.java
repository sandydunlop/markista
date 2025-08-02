package io.github.sandydunlop.markista.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageMember;

public class FileUtils {
    private static final String DEFAULT_OUTPUT_DIRECTORY = "build/md-docs";
    private String outputDirectory;
    private String flattenedDirectories = null;

    /// This constructor initializes `FileUtils` for use with the specified module.
    /// The output directory is specified here since a new `FileUtils` instance
    /// is created for each module and package.
    public FileUtils(String outputDir) {
        outputDirectory = outputDir;
    }

    public void setModule(ModuleNode moduleNode) {
        if (Configuration.getFlattenDirectories() && moduleNode != null) {
            setFlattenedDirectories(moduleNode);
        }
    }

    public String getFlattenedDirectories() {
        return flattenedDirectories;
    }

    public void setFlattenedDirectories(ModuleNode moduleNode) {
        if (!Configuration.getFlattenDirectories()) return;
        List<String> result = moduleNode.getPackages().stream().map(
                PackageMember::getName).toList();
        flattenedDirectories = commonBase(result);
        LinkResolver.setFlattenedDirectories(flattenedDirectories);
    }

    public String commonBase(List<String> packageNames) {
        if (packageNames.isEmpty()) return "";
        int lastDot = 0;
        String base = packageNames.get(0);
        for (String packageName : packageNames) {
            for (int j=0; j<Math.min(base.length(), packageName.length()); j++) {
                if (base.charAt(j) != packageName.charAt(j)) {
                    base = base.substring(0, Math.max(0,lastDot));
                    break;
                }
                if (j == packageName.length() - 1) {
                    base = base.substring(0, Math.max(0,j + 1));
                }
                if (base.charAt(j) == '.') lastDot = j;
            }
        }
        return base;
    }

    /// Utility method that counts the amounts of dots in a `String`.
    public static int countDots(String str) {
        if (Utils.isNullOrEmpty(str)) return 0;
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '.') {
                count++;
            }
        }
        return count;
    }

    /// Creates a filesystem path based on the specified output directory
    /// and the fully qualified name of a package. Each section of the package
    /// name becomes a directory unless [setFlattenedDirectories][FileUtils#setFlattenedDirectories] has been
    /// called, in which case only the directories which will contain
    /// documentation for types will be created.
    /// @param outputDirectory output path specified by the `-d` command line parameter
    /// @param packageName the name of the package being documented
    public File buildContainingDirPath(String outputDirectory, String packageName) {
        final File rootDir;
        if (outputDirectory != null) {
            rootDir = new File(outputDirectory);
        } else {
            rootDir = new File(".");
        }

        String dirName = packageName.replace('.', pathSeparator());
        if (Configuration.getFlattenDirectories() && dirName.length() > 2) {
            if (dirName.length() < flattenedDirectories.length()) {
                // This was happening when mock classes ended up mixed in
                // with what we're trying to document here
                Context.reportWarning(String.format(
                        "Unexpected path '%s' for package '%s'",
                        dirName, packageName));
            } else {
                dirName = dirName.substring(flattenedDirectories.length());
            }
        }
        return new File(rootDir, dirName);
    }

    /// Returns the character used to separate parts of a filesystem path.
    /// This method is necessary because Java's built-in `File.pathSeparatorChar`
    /// has a bug where it returns the wrong character on macOS.
    public static char pathSeparator() {
        // File.pathSeparatorChar is returning ":" on macOS (Sequoia 15.5) when it should be "/"
        return File.pathSeparatorChar == ':' ? '/' : File.pathSeparatorChar;
    }

    public Writer createFile(String className, String packageName) throws IOException{
        if (outputDirectory == null) outputDirectory = DEFAULT_OUTPUT_DIRECTORY;
        File containingDir = buildContainingDirPath(outputDirectory, packageName);
        if (!containingDir.exists()) containingDir.mkdirs();
        if (className == null) className = "index";
        File classFile = new File(containingDir, className + ".md");
        FileOutputStream fileOutputStream = new FileOutputStream(classFile);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
        return new OutputStreamWriter(bufferedOutputStream);
    }

    public Writer createModuleFile(String moduleName, String fileName) throws IOException{
        if (outputDirectory == null) outputDirectory = DEFAULT_OUTPUT_DIRECTORY;
        File containingDir = new File(outputDirectory + pathSeparator() + moduleName);
        if (!containingDir.exists()) containingDir.mkdirs();
        File moduleFile = new File(containingDir, fileName);
        FileOutputStream fileOutputStream = new FileOutputStream(moduleFile);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
        return new OutputStreamWriter(bufferedOutputStream);
    }

    public static String joinPaths(String base, String part) {
        if (base.isEmpty()) {
            return part;
        }
        if (part.isEmpty()) {
            return base;
        }
        return base + pathSeparator() + part;
    }
}
