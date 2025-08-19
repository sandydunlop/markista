package io.github.sandydunlop.markista.core;

import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.Node;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import javax.tools.Diagnostic;

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

    private Node watch = null;

    /// The default constructor
    private Context() {
        // No public constructor here
    }

    public String getFlattenedDirectories() {
        return flattenedDirectories;
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
        if (Configuration.getFlattenPackages()) {
            this.flattenedDirectories = api.commonBase();
        }
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

    public String getOutputDirectory() {
        return outputDirectory;
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
            return "\n  Location:\n" + sb;
        } else {
            return "";
        }
    }

    /// Constructs a File object representing the directory path corresponding to the current output directory,
    /// module, and fully qualified package name.
    /// If flattenDirectories is enabled, only directories containing documentation for types are created according to the flattenedDirectories prefix.
    /// @return The File object representing the directory.
    public File getPackageDirectory() {
        final File rootDir;
        if (!outputDirectory.isEmpty()) {
            rootDir = new File(outputDirectory);
        } else {
            rootDir = new File(".");
        }
        File moduleDir;
        if (Configuration.getFlattenModules()) {
            moduleDir = rootDir;
        } else {
            moduleDir = new File(rootDir, moduleName);
        }
        String dirName = packageName.replace(".", File.separator);
        if (Configuration.getFlattenPackages() && dirName.length() > 2) {
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
    public Writer createFileInPackage() throws IOException {
        File path = createPackageFilePath();
        return createFileInternal(path);
    }

    File createPackageFilePath() {
        if (outputDirectory.isEmpty()) outputDirectory = DEFAULT_OUTPUT_DIRECTORY;
        File containingDir = getPackageDirectory();
        if (!containingDir.exists()) containingDir.mkdirs();
        if (typeName.isEmpty()) typeName = "index";
        String fileName = NameSimplifier.simplifyNames(typeName);
        return new File(containingDir, fileName + ".md");
    }

    /// Creates a Writer for writing a module-level file inside the module's directory.
    /// Creates directories as necessary. Defaults of output directory are applied if not set.
    /// @param fileName The file name to create inside the module directory.
    /// @return A Writer object for writing the file.
    /// @throws IOException If an I/O error occurs creating directories or the file.
    public Writer createFileInModule(String fileName) throws InvalidPathException, IOException {
        File path = createModuleFilePath(fileName);
        return createFileInternal(path);
    }

    File createModuleFilePath(String fileName) throws InvalidPathException {
        if (outputDirectory.isEmpty()) outputDirectory = DEFAULT_OUTPUT_DIRECTORY;
        Path path;
        if (Configuration.getFlattenModules()) {
            path = Path.of(outputDirectory);
        } else {
            path = Path.of(outputDirectory, moduleName);
        }
        path = path.resolve(fileName);
        File containingDir = path.getParent().toFile();
        if (!containingDir.exists()) containingDir.mkdirs();
        return path.toFile();
    }

    /// Helper method that creates a Writer for a file inside the specified directory.
    /// @param file The filename of the file to create.
    /// @return A Writer for the file.
    /// @throws IOException If an I/O error occurs opening the file.
    private Writer createFileInternal(File file) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
        return new OutputStreamWriter(bufferedOutputStream);
    }

    public class NameSimplifier {
        private NameSimplifier() {
            // Nothing to see here
        }

        /// Changes all qualified names in a string into unqualified names.
        /// @param  str A string that may contain one or more qualified names.
        /// @return The input string, with all qualified names changed to unqualified names.
        public static String simplifyNames(String str) {
            if (str == null || str.isEmpty())
                return "";
            return simplifyNamesLoop(str);
        }

        private static String simplifyNamesLoop(String input) {
            String simplified = input;
            int qualifiedStart = -1;
            int simpleStart = -1;
            char prev = (char) 0;
            int i = 0;
            while (i <= simplified.length()) {
                char c = i < simplified.length() ? simplified.charAt(i) : ' ';
                if (shouldReplaceQualifiedWithSimple(qualifiedStart, simpleStart, i, simplified.length(), c)) {
                    simplified = replaceQualifiedWithSimple(simplified, qualifiedStart, simpleStart, i);
                    i = qualifiedStart + (i - simpleStart);
                    simpleStart = -1;
                    qualifiedStart = -1;
                } else if (qualifiedStart == -1 && isValidQualifiedNameChar(c) && !isValidSimpleNameChar(prev)) {
                    qualifiedStart = i;
                } else if (qualifiedStart > -1 && simpleStart == -1 && Character.isUpperCase(c)) {
                    simpleStart = i;
                } else if (qualifiedStart > -1 && simpleStart == -1 && !isValidQualifiedNameChar(c)) {
                    qualifiedStart = -1;
                }
                prev = c;
                i++;
            }
            return simplified;
        }

        private static boolean shouldReplaceQualifiedWithSimple(int qualifiedStart, int simpleStart, int i, int length,
                char c) {
            return qualifiedStart > -1 && simpleStart > -1 && (i == length || !isValidSimpleNameChar(c));
        }

        private static String replaceQualifiedWithSimple(String simplified, int qualifiedStart, int simpleStart, int i) {
            StringBuilder tmp = new StringBuilder();
            if (qualifiedStart > 0)
                tmp.append(simplified, 0, qualifiedStart);
            tmp.append(simplified.substring(simpleStart, i));
            if (i < simplified.length())
                tmp.append(simplified.substring(i));
            return tmp.toString();
        }

        /// Checks if the given character is valid in an unqualified name.
        /// @param c The character to check.
        /// @return  Whether or not the character is valid in an unqualified name.
        public static boolean isValidSimpleNameChar(char c) {
            return Character.isAlphabetic(c) || c == '.';
        }

        /// Checks if the given character is valid in a qualified name.
        /// @param c The character to check.
        /// @return  Whether or not the character is valid in a qualified name.
        public static boolean isValidQualifiedNameChar(char c) {
            return (Character.isAlphabetic(c) && Character.isLowerCase(c)) || c == '.';
        }
    }
}