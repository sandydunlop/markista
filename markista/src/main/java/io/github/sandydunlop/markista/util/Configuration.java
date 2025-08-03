package io.github.sandydunlop.markista.util;

/// Utility class holding global configuration settings for the documentation generator.
/// 
/// This class provides static getter and setter methods for various options that control the behavior
/// of the documentation output, such as output directory, verbosity, and which members to include.
/// 
/// All configuration fields are static and global and the constructor is hidden to prevent instantiation.
public class Configuration {
    /// The directory in which the documentation filles will be generated
    private static String outputDirectory = null;

    /// If true, links to types defined outside of the API being documented will be generated
    private static boolean createExternalLinks = false;

    /// If true, private members of packages and types will be included in the generated documentation
    private static boolean documentPrivateMembers = false;

    /// If true, empty directories will not be created, leading to a flatter directory structure
    private static boolean flattenDirectories = false;

    /// If true, summary tables will be created inside Markdown content tabs.
    /// See [mkdocs-material](https://squidfunk.github.io/mkdocs-material/reference/content-tabs/) for more information.
    private static boolean useContentTabs = false;

    /// If true, logging will include status information.
    private static boolean verbose = false;

    /// The default constructor
    private Configuration() {
        // Hide the public constructor
    }

    /// Sets the directory path where the documentation files will be generated.
    /// @param path The output directory path as a String.
    public static void setOutputDirectory(String path) {
        outputDirectory = path;
    }

    /// Returns the directory path where documentation files will be generated.
    /// @return The output directory path as a String.
    public static String getOutputDirectory() {
        return outputDirectory;
    }

    /// Sets whether to generate links to external documentation for referenced types and modules.
    /// @param b true to create external links, false otherwise.
    public static void setCreateExternalLinks(boolean b) {
        createExternalLinks = b;
    }

    /// Returns whether the documentation generator creates links to external documentation.
    /// @return true if external links should be created, false otherwise.
    public static boolean getCreateExternalLinks() {
        return createExternalLinks;
    }

    /// Sets whether private members (fields, methods) should be included in the generated documentation.
    /// @param b true to include private members, false otherwise.
    public static void setDocumentPrivateMembers(boolean b) {
        documentPrivateMembers = b;
    }

    /// Returns whether private members are included in the documentation.
    /// @return true if private members are included, false otherwise.
    public static boolean getDocumentPrivateMembers() {
        return documentPrivateMembers;
    }

    /// Sets whether to flatten directory structure when generating the documentation output.
    /// @param b true to flatten directories, false otherwise.
    public static void setFlattenDirectories(boolean b) {
        flattenDirectories = b;
    }

    /// Returns whether the documentation output directory structure is flattened.
    /// @return true if directories are flattened, false otherwise.
    public static boolean getFlattenDirectories() {
        return flattenDirectories;
    }

    /// Sets whether to use content tabs in the generated documentation.
    /// @param b true to use content tabs, false otherwise.
    public static void setUseContentTabs(boolean b) {
        useContentTabs = b;
    }

    /// Returns whether content tabs are used in documentation.
    /// @return true if content tabs are enabled, false otherwise.
    public static boolean getUseContentTabs() {
        return useContentTabs;
    }

    /// Sets whether verbose logging or output should be enabled during documentation generation.
    /// @param b true to enable verbose output, false otherwise.
    public static void setVerbose(boolean b) {
        verbose = b;
    }

    /// Returns whether verbose output is enabled.
    /// @return true if verbose output is enabled, false otherwise.
    public static boolean getVerbose() {
        return verbose;
    }
}