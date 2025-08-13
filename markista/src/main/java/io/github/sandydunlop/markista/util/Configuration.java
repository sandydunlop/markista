package io.github.sandydunlop.markista.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/// Utility class holding global configuration settings for the documentation generator.
/// 
/// This class provides static getter and setter methods for various options that control the behavior
/// of the documentation output, such as output directory, verbosity, and which members to include.
/// 
/// All configuration fields are static and global and the constructor is hidden to prevent instantiation.
public class Configuration {
    /// The title of the API documentation being generated
    private static String docTitle = "API";

    /// If true, links to types defined outside the API being documented will be generated
    private static boolean createExternalLinks = false;

    /// If true, private members of packages and types will be included in the generated documentation
    private static boolean documentPrivateMembers = false;

    /// If true, empty package directories will not be created, leading to a flatter directory structure
    private static boolean flattenPackages = false;

    /// If true, module directories will not be created, leading to a flatter directory structure
    private static boolean flattenModules = false;

    /// If true, summary tables will be created inside Markdown content tabs.
    /// See [mkdocs-material](https://squidfunk.github.io/mkdocs-material/reference/content-tabs/) for more information.
    private static boolean useContentTabs = false;

    /// If true, logging will include status information.
    private static boolean verbose = false;

    /// The list of module paths passed in from the Javadoc command line
    private static List<String> modulePathList = new ArrayList<>();

    /// The list of modules that have local Javadoc that can be linked to
    private static List<String> linkExternal = new ArrayList<>();

    private static String projectPath = null;

    /// The default constructor
    private Configuration() {
        // Hide the public constructor
    }

    /// Sets the title used in the generated documentation
    /// @param title the title to use
    public static void setDocTitle(String title) {
        docTitle = title;
    }

    /// Returns the title used in the generated documentation
    /// @return The title to be used
    public static String getDocTitle() {
        return docTitle;
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

    /// Sets whether to flatten package directory structure when generating the documentation output.
    /// @param b true to flatten package directories, false otherwise.
    public static void setFlattenPackages(boolean b) {
        flattenPackages = b;
    }

    /// Returns whether the documentation output package directory structure is flattened.
    /// @return true if package directories are flattened, false otherwise.
    public static boolean getFlattenPackages() {
        return flattenPackages;
    }

    /// Sets whether to flatten module directory structure when generating the documentation output.
    /// @param b true to flatten module directories, false otherwise.
    public static void setFlattenModules(boolean b) {
        flattenModules = b;
    }

    /// Returns whether the documentation output module directory structure is flattened.
    /// @return true if module directories are flattened, false otherwise.
    public static boolean getFlattenModules() {
        return flattenModules;
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

    /// Sets the list of sibling modules that can be linked to
    /// @param moduleList a colon-separated string of module names
    public static void setLinkExternal(String moduleList) {
        String[] modules = moduleList.split(":");
        linkExternal = Arrays.asList(modules);
    }

    /// Gets the list of sibling modules that can be linked to
    /// @return a list of sibling modules that can be linked to
    public static List<String> getListExternal() {
        return linkExternal;
    }

    /// Sets the directory containing modules.
    /// @param modulePaths A colon-separated list of paths.
    public static void setModulePaths(String modulePaths) {
        String[] pathList = modulePaths.split(":");
        modulePathList = Arrays.asList(pathList);
    }

    /// Gets the list of modules
    /// @return A list of the module paths
    public static List<String> getModulePaths() {
        return modulePathList;
    }

    public static void setProjectPath(String path) {
        projectPath = path;
    }

    public static String getProjectPath() {
        return projectPath;
    }
}