package io.github.sandydunlop.markista.doclet;
 
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.util.Configuration;
import io.github.sandydunlop.markista.util.LinkResolver;
import io.github.sandydunlop.markista.util.ModuleDirectiveGenerator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic;
import javax.tools.DocumentationTool;
import javax.tools.ToolProvider;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

/// A doclet that renders javadoc comments as Markdown
public class MarkdownDoclet implements Doclet {
    public static final boolean OK = true;
    private static final boolean FAILED = false; 
    private static final String DOT_HTML = ".html";
    private static final String JAVA_24_URL = "https://docs.oracle.com/en/java/javase/24/docs/api/";

    /// The default constructor, does nothing.
    public MarkdownDoclet() {
        // Nothing to see here
    }

    /// The starting point of Markista if it is being used
    /// without the Javadoc command. This is useful for debugging.
    /// @param args this parameter is ignored.
    public static void main(String[] args) {
        String[] docletArgs = new String[]{
            "-doclet", MarkdownDoclet.class.getName(),
            "-docletpath", "build/classes/java/main", 
            "-d", "build/md-docs", 
            "-private", 
            "-external",
            "-flatten",

            "-sourcepath", "src/main/java/",
            "-subpackages", "io.github.sandydunlop",

            // "--module-source-path", "src/main/java",
            // "--module-path", "build/libs",
            // "--module", "markista",

            // "--module-source-path", "/Users/sandy/git/cu/dev/food-example/*/src/main/java",
            // "--module-path", "../food-example/*/build/libs",
            // "--module", "serviceinterface,consumer,provider",

            "-verbose"
        };
        DocumentationTool docTool = ToolProvider.getSystemDocumentationTool();
        docTool.run(System.in, System.out, System.err, docletArgs); //NOSONAR
    }

    /// A base class for declaring options.
    /// Subtypes for specific options should implement
    /// the [process][#process(String,List)] method
    /// to handle instances of the option found on the
    /// command line.
    public abstract class Option implements Doclet.Option {
        private final String name;
        private final boolean hasArg;
        private final String description;
        private final String parameters;
 
        Option(String name, boolean hasArg,
               String description, String parameters) {
            this.name = name;
            this.hasArg = hasArg;
            this.description = description;
            this.parameters = parameters;
        }
 
        @Override
        public int getArgumentCount() {
            return hasArg ? 1 : 0;
        }
 
        @Override
        public String getDescription() {
            return description;
        }
 
        @Override
        public Kind getKind() {
            return Kind.STANDARD;
        }
 
        @Override
        public List<String> getNames() {
            return List.of(name);
        }
 
        @Override
        public String getParameters() {
            return hasArg ? parameters : "";
        }
    }
 
    private static final String UNUSED_OPTION_DESCRIPTION = "Unused option";

    private final Set<Option> options = Set.of(
            new Option("-d", true,
                    "output directory", null) {
                @Override
                public boolean process(String option,
                                       List<String> arguments) {
                    if (arguments != null && !arguments.isEmpty()) {
                        Configuration.setOutputDirectory(arguments.get(0));
                    }
                    return OK;
                }
            },
            new Option("-doctitle", true,
                    UNUSED_OPTION_DESCRIPTION, null) {
                @Override
                public boolean process(String option,
                                       List<String> arguments) {
                    if (arguments != null && !arguments.isEmpty()) {
                        // Do nothing
                    }
                    return OK;
                }
            },
            new Option("-external", false,
                    "create external links", null) {
                @Override
                public boolean process(String option,
                                       List<String> arguments) {
                    Configuration.setCreateExternalLinks(true);
                    return OK;
                }
            },
            new Option("-flatten", false,
                    "prevents directories for empty packages being created", null) {
                @Override
                public boolean process(String option,
                                       List<String> arguments) {
                    Configuration.setFlattenDirectories(true);
                    return OK;
                }
            },
            new Option("-notimestamp", false,
                    UNUSED_OPTION_DESCRIPTION, null) {
                @Override
                public boolean process(String option,
                                       List<String> arguments) {
                    if (arguments != null && !arguments.isEmpty()) {
                        // Do nothing
                    }
                    return OK;
                }
            },
            new Option("-private", false,
                    "View all classes and members.", null) {
                @Override
                public boolean process(String option,
                                       List<String> arguments) {
                    Configuration.setDocumentPrivateMembers(true);
                    return OK;
                }
            },
            new Option("-quiet", false,
                    UNUSED_OPTION_DESCRIPTION, null) {
                @Override
                public boolean process(String option,
                                       List<String> arguments) {
                    if (arguments != null && !arguments.isEmpty()) {
                        // Do nothing
                    }
                    return OK;
                }
            },
            new Option("-verbose", false,
                    "Output informational messages", null) {
                @Override
                public boolean process(String option,
                                       List<String> arguments) {
                    Configuration.setVerbose(true);
                    return OK;
                }
            },
            new Option("-windowtitle", true,
                    UNUSED_OPTION_DESCRIPTION, null) {
                @Override
                public boolean process(String option,
                                       List<String> arguments) {
                    if (arguments != null && !arguments.isEmpty()) {
                        // Do nothing
                    }
                    return OK;
                }
            }
    );

    /// Initializes the doclet.
    /// @param locale The locale used for messages
    /// @param reporter The reporter used for messages
    @Override
    public void init(Locale locale, Reporter reporter) {
        Configuration.setReporter(reporter);
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
 
    @Override
    public Set<? extends Option> getSupportedOptions() {
        return options;
    }
 
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }
 
    /// @param environment Represents the operating environment of a single invocation of the doclet. 
    /// @return  true if completed without errors, false if errors occurred.
    @Override
    public boolean run(DocletEnvironment environment) {
        if (Configuration.getCreateExternalLinks()) {
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            InputStream inputStream = classloader.getResourceAsStream("java_platform_modules.text");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String moduleName;
                while ((moduleName = reader.readLine()) != null) {
                    addNativeModule(moduleName);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        ModuleDirectiveGenerator.setEnvironment(environment);
        ApiScanner scanner = new ApiScanner(environment);
        Api api = scanner.scan(environment.getIncludedElements());
        api.sort();
        LinkResolver.setApi(api);

        ModuleWriter writer = new ModuleWriter();
        try{
            writer.writeDocs(api);
        } catch (IOException ex) {
            Configuration.getReporter().print(Diagnostic.Kind.ERROR, ex.getMessage());
            Configuration.getReporter().print(Diagnostic.Kind.ERROR, Arrays.toString(ex.getStackTrace()));
            return FAILED;
        }
        return OK;
    }

    private void addNativeModule(String moduleName) {
        // Tell the link resolver what web address to find docs for certain Java modules at
        LinkResolver.addNativeModule(moduleName, JAVA_24_URL + moduleName, DOT_HTML);
    }
}