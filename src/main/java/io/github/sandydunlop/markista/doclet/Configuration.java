package io.github.sandydunlop.markista.doclet;

import jdk.javadoc.doclet.Reporter;

public class Configuration {
    private static Reporter reporter;
    private static String outputDirectory = null;
    private static boolean documentPrivateMembers = false;
    private static boolean createExternalLinks = false;
    private static boolean flattenDirectories = false;
    private static boolean verbose = false;

    private Configuration() {
        // Hide the public constructor
    }

    public static void setReporter(Reporter r) {
        reporter = r;
    }

    public static Reporter getReporter() {
        return reporter;
    }

    public static void setOutputDirectory(String path) {
        outputDirectory = path;
    }

    public static String getOutputDirectory() {
        return outputDirectory;
    }

    public static void setDocumentPrivateMembers(boolean b) {
        documentPrivateMembers = b;
    }

    public static boolean getDocumentPrivateMembers() {
        return documentPrivateMembers;
    }

    public static void setCreateExternalLinks(boolean b) {
        createExternalLinks = b;
    }

    public static boolean getCreateExternalLinks() {
        return createExternalLinks;
    }

    public static void setFlattenDirectories(boolean b) {
        flattenDirectories = b;
    }

    public static boolean getFlattenDirectories() {
        return flattenDirectories;
    }

    public static void setVerbose(boolean b) {
        verbose = b;
    }

    public static boolean getVerbose() {
        return verbose;
    }
}
