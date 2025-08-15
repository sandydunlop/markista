package io.github.sandydunlop.markista.doclet;
 
import io.github.sandydunlop.markista.core.Configuration;
import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.markdown.ModuleWriter;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.spi.DocService;
import io.github.sandydunlop.markista.util.ApiScanner;
import io.github.sandydunlop.markista.util.LinkFormatter;
import io.github.sandydunlop.markista.util.LinkResolver;
import io.github.sandydunlop.markista.util.ModuleDirectives;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import javax.lang.model.SourceVersion;
import javax.tools.DocumentationTool;
import javax.tools.ToolProvider;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

/// A doclet that renders javadoc comments as Markdown
public class MarkdownDoclet implements Doclet {
    /// The Context singleton instance providing access to the current documentation generation context,
    /// including configuration, current module/package/type names, and reporting utilities.
    private Context ctx;

    /// Indicates success
    public static final boolean OK = true;

    /// Indicates failure
    private static final boolean FAILED = false; 

    static String[] docletArgs = null;
    static List<String> javadocArgs = new ArrayList<>();

    /// The default constructor, does nothing.
    public MarkdownDoclet() {
        // Nothing to see here
    }

    /// Reads a javadoc.options file containing command line arguments for Javadoc.
    /// @param optionsPath the path of the file to be read
    public static void readJavadocOptions(String optionsPath) {
        try{
            byte[] bytes = Files.readAllBytes(Paths.get(optionsPath));
            String content = new String(bytes);
            String[] parts = content.split(",");
            for (String part : parts) {
                part = part.strip();
                if (part.length() > 2) {
                    javadocArgs.add(part.substring(1, part.length() - 1));
                }
            }
        } catch (IOException _) {
            System.err.println("Unable to load options file: " + optionsPath); //NOSONAR - There is no other way to do this
        }
    }

    /// The starting point of Markista if it is being used
    /// without the Javadoc command. This is useful for debugging.
    /// @param args this parameter is ignored.
    public static void main(String[] args) {
        if (args != null && args.length > 1 && args[0].equals("-options")) {
            readJavadocOptions(args[1]);
        }
        if (!javadocArgs.isEmpty()) {
            javadocArgs.addFirst("build/classes/java/main");
            javadocArgs.addFirst("-docletpath");
            javadocArgs.addFirst(MarkdownDoclet.class.getName());
            javadocArgs.addFirst("-doclet");
            docletArgs = javadocArgs.toArray(new String[0]);
        } else {
            docletArgs = new String[]{
                "-doclet", MarkdownDoclet.class.getName(),
                "-docletpath", "build/classes/java/main", 
                "-d", "markista/build/md-docs", 
                "-private", 
                "-link",
                "-doctitle", "Markista API",
                "--flatten-packages",
                "-sourcepath", "markista/src/main/java/",
                "-subpackages", "io.github.sandydunlop.markista",
                "-tabs"
//                "-verbose"
            };
        }
        DocumentationTool docTool = ToolProvider.getSystemDocumentationTool();
        docTool.run(System.in, System.out, System.err, docletArgs); //NOSONAR
    }

    /// A base class for declaring options.
    /// Subtypes for specific options should implement
    /// the [process][jdk.javadoc.doclet.Doclet.Option#process(java.lang.String,java.util.List)] method
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
                        ctx.setOutputDirectory(arguments.getFirst());
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
                        Configuration.setDocTitle(arguments.getFirst());
                    }
                    return OK;
                }
            },
            new Option("--flatten-modules", false,
                    "prevents individual directories for modules being created", null) {
                @Override
                public boolean process(String option,
                                       List<String> arguments) {
                    Configuration.setFlattenModules(true);
                    return OK;
                }
            },
            new Option("--flatten-packages", false,
                    "prevents directories for empty packages being created", null) {
                @Override
                public boolean process(String option,
                                       List<String> arguments) {
                    Configuration.setFlattenPackages(true);
                    return OK;
                }
            },
            new Option("-link", false,
                    "create external links", null) {
                @Override
                public boolean process(String option,
                                       List<String> arguments) {
                    Configuration.setCreateExternalLinks(true);
                    return OK;
                }
            },
            new Option("--link-modules", true,
                    "Specifies a list of modules with javadoc that can be linked to.", null) {
                @Override
                public boolean process(String option,
                                       List<String> arguments) {
                    if (arguments != null && !arguments.isEmpty()) {
                        Configuration.setLinkExternal(arguments.getFirst());
                    }
                    return OK;
                }
            },
            new Option("--module-path", true,
                    "Specifies where to find application modules.", null) {
                @Override
                public boolean process(String option,
                                       List<String> arguments) {
                    if (arguments != null && !arguments.isEmpty()) {
                        Configuration.setModulePaths(arguments.getFirst());
                    }
                    return OK;
                }
            },
            new Option("-notimestamp", false,
                    UNUSED_OPTION_DESCRIPTION, null) {
                @Override
                public boolean process(String option,
                                       List<String> arguments) {
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
            new Option("--project-path", true,
                    "The root directory of the project.", null) {
                @Override
                public boolean process(String option,
                                       List<String> arguments) {
                    if (arguments != null && !arguments.isEmpty()) {
                        Configuration.setProjectPath(arguments.getFirst());
                    }
                    return OK;
                }
            },
            new Option("-quiet", false,
                    UNUSED_OPTION_DESCRIPTION, null) {
                @Override
                public boolean process(String option,
                                       List<String> arguments) {
                    return OK;
                }
            },
            new Option("-tabs", false,
                    "Show summary tables in content tabs", null) {
                @Override
                public boolean process(String option,
                                       List<String> arguments) {
                    Configuration.setUseContentTabs(true);
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
                    return OK;
                }
            }
    );

    /// Initializes the doclet.
    /// @param locale The locale used for messages
    /// @param reporter The reporter used for messages
    @Override
    public void init(Locale locale, Reporter reporter) {
        ctx = Context.getInstance();
        ctx.setReporter(reporter);
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
 
    /// The `run` method is called by the Javadoc tool to begin running the doclet.
    /// @param environment Represents the operating environment of a single invocation of the doclet. 
    /// @return  true if completed without errors, false if errors occurred.
    @Override
    public boolean run(DocletEnvironment environment) {
        ModuleDirectives.setEnvironment(environment);
        ApiScanner scanner = new ApiScanner(environment);
        Api api = scanner.scan(environment.getIncludedElements());
        ctx.setApi(api);
        LinkResolver.setFlattenedDirectories(ctx.getFlattenedDirectories());
        LinkResolver.init(api, ctx);
        if (Configuration.getCreateExternalLinks()) {
            LinkResolver.addNativeModules();
        }
        LinkFormatter.generateLinkTexts(api, ctx);

        // The Writer used to output the generated markdown content for the current document.
        // It handles writing text to the appropriate output file or stream.
        ModuleWriter writer = new ModuleWriter();
        try{
            writer.writeDocs(api);

            // Run any modules providing a DocService implementation
            ServiceLoader<DocService> loader = ServiceLoader.load(DocService.class);
            for (DocService docServiceImplementation : loader) {
                docServiceImplementation.run(api, ctx);
            }
        } catch (IOException ex) {
            ctx.reportError(ex.getMessage() + "\n" + Arrays.toString(ex.getStackTrace()));
            return FAILED;
        }
        return OK;
    }
}