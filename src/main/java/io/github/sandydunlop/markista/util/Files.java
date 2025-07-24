package io.github.sandydunlop.markista.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import io.github.sandydunlop.markista.doclet.Configuration;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageMember;

public class Files {
    private String outputDirectory;
    private String squashedDirectories = null;

    public Files(ModuleNode moduleNode, String outputDir) {
        outputDirectory = outputDir;
        if (Configuration.getFlattenDirectories() && moduleNode != null) {
            setSquashedDirectories(moduleNode);
        }
    }

    public String getFlattenedDirectories() {
        return squashedDirectories;
    }

    public void setSquashedDirectories(ModuleNode moduleNode) {
        squashedDirectories = null;
        int dotCount = 0;
        if (Configuration.getFlattenDirectories()) {
            for (PackageMember packageNode : moduleNode.getPackages()) {
                if (squashedDirectories == null) {
                    squashedDirectories = packageNode.getName();
                    dotCount = countDots(squashedDirectories);
                } else if (countDots(packageNode.getName()) < dotCount) {
                    dotCount = countDots(packageNode.getName());
                    squashedDirectories = packageNode.getName();
                }
            }
        }
        if (squashedDirectories != null && squashedDirectories.lastIndexOf('.') > -1) {
            squashedDirectories = squashedDirectories.substring(0, squashedDirectories.lastIndexOf('.'));
            LinkResolver.setSquashedDirectories(squashedDirectories);
        }
    }

    private int countDots(String str) {
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
           dirName = dirName.substring(squashedDirectories.length());
        }
        return new File(rootDir, dirName);
    }

    public char pathSeparator() {
        // File.pathSeparatorChar is returnng ":" on macOS (Sequoia 15.5) when it should be "/"
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
