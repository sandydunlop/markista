package io.github.sandydunlop.markista.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;

import javax.tools.Diagnostic;

import io.github.sandydunlop.markista.model.Api;
import jdk.javadoc.doclet.Reporter;

/// Singleton class that maintains the current context during documentation generation.
/// 
/// This class tracks the current API model, output directory, current module, package, type, method, and field names,
/// as well as the reporter used for reporting messages such as INFO, WARNING, and ERROR.
/// 
/// It provides utilities to create directories and files for output, report errors or warnings with location information,
/// and manage the structure of output paths in relation to package and module names.
/// 
/// The Context instance should be obtained via getInstance(), and its fields configured as the documentation generation progresses.
/// This class is not thread-safe.
public class Context { //NOSONAR - This works best as a singleton but Sonar shows that as a warning
    /// The default output directory
    private static final String DEFAULT_OUTPUT_DIRECTORY = "build/md-docs";

    /// The instance of this class that is returned to callers of getInstance()
    private static Context instance;

    /// The [Reporter] used for logging messages
    private Reporter reporter;

    /// The Api model representing the entire documented API structure,
    /// including modules, packages, types, and members used for cross-referencing and navigation.
    private Api api;

    /// The directory the documentation is being generated in
    private String outputDirectory = "";

    /// The base section of the directory structure that contains no 
    /// documentation and can be skipped when creating directories.
    private String flattenedDirectories = "";

    /// The name of the module currently being documented
    private String moduleName = "";

    /// The name of the package currently being documented
    private String packageName = "";

    /// The name of the type currently being documented
    private String typeName = "";

    /// The name of the method currently being documented
    private String methodName = "";

    /// The name of the field currently being documented
    private String fieldName = "";

    /// The default constructor
    private Context() {
        // No public constructor here
    }

    /// Returns the singleton instance of this Context.
    /// @return The Context singleton instance.
    public static Context getInstance() {
        if (instance == null) {
            instance = new Context();
        }
        return instance;
    }

    /// Sets the Api model associated with this context, also initializing the flattened directories string.
    /// @param api The Api instance.
    public void setApi(Api api) {
        this.api = api;
        this.flattenedDirectories = api.commonBase();
    }

    /// Returns the Api model associated with this context.
    /// @return The current Api model.
    public Api getApi() {
        return api;
    }

    /// Sets the output directory path where documentation files will be written.
    /// @param outputDirectory The output directory path as a String.
    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory == null ? "" : outputDirectory;
    }

    /// Sets the reporter used for reporting messages and diagnostics during doc generation.
    /// @param reporter The Reporter instance.
    public void setReporter(Reporter reporter) {
        this.reporter = reporter;
    }

    /// Returns the currently set reporter instance.
    /// @return The Reporter.
    public Reporter getReporter() {
        return reporter;
    }

    /// Sets the current module name and resets the package name.
    /// @param name The module name to set.
    public void setModuleName(String name) {
        setPackageName("");
        moduleName = name == null ? "" : name;
    }

    /// Returns the current module name.
    /// @return The module name.
    public String getModuleName() {
        return moduleName;
    }

    /// Sets the current package name and resets the type name.
    /// @param name The package name to set.
    public void setPackageName(String name) {
        setTypeName("");
        packageName = name == null ? "" : name;
    }

    /// Returns the current package name.
    /// @return The package name.
    public String getPackageName() {
        return packageName;
    }

    /// Sets the current type name.
    /// @param name The type name to set.
    public void setTypeName(String name) {
        typeName = name == null ? "" : name;
    }

    /// Returns the current type name.
    /// @return The type name.
    public String getTypeName() {
        return typeName;
    }

    /// Sets the current method name.
    /// @param name The method name to set.
    public void setMethodName(String name) {
        methodName = name == null ? "" : name;
    }

    /// Returns the current method name.
    /// @return The method name.
    public String getMethodName() {
        return methodName;
    }

    /// Sets the current field name.
    /// @param name The field name to set.
    public void setFieldName(String name) {
        fieldName = name == null ? "" : name;
    }

    /// Returns the current field name.
    /// @return The field name.
    public String getFieldName() {
        return fieldName;
    }

    /// Reports an informational message through the reporter.
    /// @param message The message to report.
    public void reportInfo(String message) {
        reporter.print(Diagnostic.Kind.NOTE, message);
    }

    /// Reports a warning message including location information.
    /// @param message The warning message to report.
    public void reportWarning(String message) {
        reporter.print(Diagnostic.Kind.WARNING, message + location());
    }

    /// Reports an error message including location information.
    /// @param message The error message to report.
    public void reportError(String message) {
        reporter.print(Diagnostic.Kind.ERROR, message + location());
    }

    /// Returns a string describing the current location context in module, package, type, method, and field.
    /// Used to append context details to diagnostic messages.
    /// @return A formatted multi-line string describing the current location, or empty if no location info.
    private String location() {
        StringBuilder sb = new StringBuilder();
        if (!moduleName.isEmpty()) {
            sb.append("    [ Module] ");
            sb.append(moduleName);
            sb.append("\n");
        }
        if (!packageName.isEmpty()) {
            sb.append("    [Package] ");
            sb.append(packageName);
            sb.append("\n");
        }
        if (!typeName.isEmpty()) {
            sb.append("    [   Type] ");
            sb.append(typeName);
            sb.append("\n");
        }
        if (!methodName.isEmpty()) {
            sb.append("    [ Method] ");
            sb.append(methodName);
            sb.append("\n");
        }
        if (!fieldName.isEmpty()) {
            sb.append("    [  Field] ");
            sb.append(fieldName);
            sb.append("\n");
        }
        if (!sb.isEmpty()) {
            return "\n  Location:\n" + sb.toString();
        } else {
            return "";
        }
    }

    /// Constructs a File object representing the directory path corresponding to the current output directory,
    /// module, and fully qualified package name.
    /// If flattenDirectories is enabled, only directories containing documentation for types are created according to the flattenedDirectories prefix.
    /// @return The File object representing the directory.
    public File getDirectory() {
        final File rootDir;
        if (!outputDirectory.isEmpty()) {
            rootDir = new File(outputDirectory);
        } else {
            rootDir = new File(".");
        }
        File moduleDir = new File(rootDir, moduleName);
        String dirName = packageName.replace('.', pathSeparator());
        if (Configuration.getFlattenDirectories() && dirName.length() > 2) {
            if (dirName.length() < flattenedDirectories.length()) {
                // This was happening when mock classes ended up mixed in
                // with what we're trying to document here
                reportWarning(String.format(
                        "Unexpected path '%s' for package '%s'",
                        dirName, packageName));
            } else {
                dirName = dirName.substring(flattenedDirectories.length());
            }
        }
        return new File(moduleDir, dirName);
    }

    /// Creates a Writer for writing a file corresponding to the current context (type name, package, module).
    /// Creates directories as necessary. Defaults of output directory are applied if not set.
    /// @return A Writer object for writing the file.
    /// @throws IOException If an I/O error occurs creating the directories or file.
    public Writer createFile() throws IOException {
        if (outputDirectory.isEmpty()) outputDirectory = DEFAULT_OUTPUT_DIRECTORY;
        File containingDir = getDirectory();
        if (!containingDir.exists()) containingDir.mkdirs();
        if (typeName.isEmpty()) typeName = "index";
        return createFileInternal(containingDir, Utils.simplifyNames(typeName) + ".md");
    }

    /// Creates a Writer for writing a module-level file inside the module's directory.
    /// Creates directories as necessary. Defaults of output directory are applied if not set.
    /// @param fileName The file name to create inside the module directory.
    /// @return A Writer object for writing the file.
    /// @throws IOException If an I/O error occurs creating directories or the file.
    public Writer createModuleFile(String fileName) throws IOException {
        if (outputDirectory.isEmpty()) outputDirectory = DEFAULT_OUTPUT_DIRECTORY;
        File containingDir = new File(outputDirectory, moduleName);
        if (!containingDir.exists()) containingDir.mkdirs();
        return createFileInternal(containingDir, fileName);
    }

    /// Helper method that creates a Writer for a file inside the specified directory.
    /// @param containingDir The directory where the file is created.
    /// @param fileName The filename of the file to create.
    /// @return A Writer for the file.
    /// @throws IOException If an I/O error occurs opening the file.
    private Writer createFileInternal(File containingDir, String fileName) throws IOException {
        File file = new File(containingDir, fileName);
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
        return new OutputStreamWriter(bufferedOutputStream);
    }

    /// Returns the character used to separate parts of filesystem paths for output directory construction.
    /// This method works around a macOS bug where `File.pathSeparatorChar` wrongly returns ':' instead of '/'.
    /// @return The platform-appropriate character for separating path components.
    private char pathSeparator() {
        // File.pathSeparatorChar is returning ":" on macOS (Sequoia 15.5) when it should be "/"
        return File.pathSeparatorChar == ':' ? '/' : File.pathSeparatorChar;
    }
}