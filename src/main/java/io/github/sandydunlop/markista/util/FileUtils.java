package io.github.sandydunlop.markista.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageMember;

public class FileUtils {
    private String outputDirectory;
    private String flattenedDirectories = null;

    public FileUtils(ModuleNode moduleNode, String outputDir) {
        outputDirectory = outputDir;
        if (Configuration.getFlattenDirectories() && moduleNode != null) {
            setFlattenedDirectories(moduleNode);
        }
    }

    public String getFlattenedDirectories() {
        return flattenedDirectories;
    }

    public void setFlattenedDirectories(ModuleNode moduleNode) {
        flattenedDirectories = null;
        int dotCount = 0;
        if (Configuration.getFlattenDirectories()) {
            for (PackageMember packageNode : moduleNode.getPackages()) {
                if (flattenedDirectories == null) {
                    flattenedDirectories = packageNode.getName();
                    dotCount = countDots(flattenedDirectories);
                } else if (countDots(packageNode.getName()) < dotCount) {
                    dotCount = countDots(packageNode.getName());
                    flattenedDirectories = packageNode.getName();
                }
            }
        }
        if (flattenedDirectories != null && flattenedDirectories.lastIndexOf('.') > -1) {
            flattenedDirectories = flattenedDirectories.substring(0, flattenedDirectories.lastIndexOf('.'));
            LinkResolver.setSquashedDirectories(flattenedDirectories);
        }
    }

    public int countDots(String str) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '.') {
                count++;
            }
        }
        return count;
    }

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
           dirName = dirName.substring(flattenedDirectories.length());
        }
        return new File(rootDir, dirName);
    }

    public char pathSeparator() {
        // File.pathSeparatorChar is returning ":" on macOS (Sequoia 15.5) when it should be "/"
        return File.pathSeparatorChar == ':' ? '/' : File.pathSeparatorChar;
    }

    public Writer createFile(String className, String packageName) throws IOException{
        File containingDir = buildContainingDirPath(outputDirectory, packageName);
        if (!containingDir.exists()) containingDir.mkdirs();
        if (className == null) className = "index";
        File classFile = new File(containingDir, className + ".md");
        FileOutputStream fileOutputStream = new FileOutputStream(classFile);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
        return new OutputStreamWriter(bufferedOutputStream);
    }

    public Writer createModuleFile(String moduleName, String fileName) throws IOException{
        File containingDir = new File(outputDirectory + pathSeparator() + moduleName);
        if (!containingDir.exists()) containingDir.mkdirs();
        File moduleFile = new File(containingDir, fileName);
        FileOutputStream fileOutputStream = new FileOutputStream(moduleFile);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
        return new OutputStreamWriter(bufferedOutputStream);
    }
}
