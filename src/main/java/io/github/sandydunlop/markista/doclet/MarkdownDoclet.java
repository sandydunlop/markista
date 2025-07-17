package io.github.sandydunlop.markista.doclet;
 
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.util.LinkResolver;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.lang.model.SourceVersion;
import javax.tools.DocumentationTool;
import javax.tools.ToolProvider;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

/// A doclet that renders javadoc comments as Markdown
public class MarkdownDoclet implements Doclet {
    private static final boolean OK = true;
    private static final boolean FAILED = false;
    private String outputDirectory = null;
    private boolean documentPrivateMembers = false;
    private boolean createExternalLinks = false;

    /// The default constructor, does nothing.
    public MarkdownDoclet() {
        // Nothing to see here
    }

    public static void main(String[] args) {
        String[] docletArgs = new String[]{
            "-doclet", MarkdownDoclet.class.getName(),
            "-docletpath", "build/classes/java/main", 
            "-d", "build/md-docs", 
            "--private", 
            "--external-links",
            "-sourcepath", "src/main/java/", 
            "-subpackages", 
            "io.github.sandydunlop"
        };
        DocumentationTool docTool = ToolProvider.getSystemDocumentationTool();
        docTool.run(System.in, System.out, System.err, docletArgs);
    }

    /// A base class for declaring options.
    /// Subtypes for specific options should implement
    /// the [process][#process(String,List)] method
    /// to handle instances of the option found on the
    /// command line.
    abstract class Option implements Doclet.Option {
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
 
    private final Set<Option> options = Set.of(
            new Option("-d", true,
                    "output directory", null) {
                @Override
                public boolean process(String option,
                                       List<String> arguments) {
                    if (arguments != null && !arguments.isEmpty()) {
                        outputDirectory = arguments.get(0);
                    }
                    return OK;
                }
            },
            new Option("--external-links", false,
                    "create external links", null) {
                @Override
                public boolean process(String option,
                                       List<String> arguments) {
                    createExternalLinks = true;
                    return OK;
                }
            },
            new Option("--private", false,
                    "include members with private modifier", null) {
                @Override
                public boolean process(String option,
                                       List<String> arguments) {
                    documentPrivateMembers = true;
                    return OK;
                }
            },
            // The following options aren't used, but are needed for compatibility with Gradle
            new Option("-doctitle", true,
                    "unused", null) {
                @Override
                public boolean process(String option,
                                       List<String> arguments) {
                    if (arguments != null && !arguments.isEmpty()) {
                        // Do nothing
                    }
                    return OK;
                }
            },
            new Option("-notimestamp", false,
                    "unused", null) {
                @Override
                public boolean process(String option,
                                       List<String> arguments) {
                    if (arguments != null && !arguments.isEmpty()) {
                        // Do nothing
                    }
                    return OK;
                }
            },
            new Option("-quiet", false,
                    "unused", null) {
                @Override
                public boolean process(String option,
                                       List<String> arguments) {
                    if (arguments != null && !arguments.isEmpty()) {
                        // Do nothing
                    }
                    return OK;
                }
            },
            new Option("-windowtitle", true,
                    "unused", null) {
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
        // Nothing to see here
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
        if (createExternalLinks) {
            // Tell the link resolver what web address to find docs for certain Java modules at
            final String DOT_HTML = ".html";
            final String JAVA_24_URL = "https://docs.oracle.com/en/java/javase/24/docs/api/";
            LinkResolver.addNativeModule("java.base", JAVA_24_URL + "java.base", DOT_HTML);
            LinkResolver.addNativeModule("java.compiler", JAVA_24_URL + "java.compiler", DOT_HTML);
            LinkResolver.addNativeModule("java.desktop", JAVA_24_URL + "java.desktop", DOT_HTML);
            LinkResolver.addNativeModule("jdk.javadoc", JAVA_24_URL + "jdk.javadoc", DOT_HTML);
            LinkResolver.addNativeModule("jdk.compiler", JAVA_24_URL + "jdk.compiler", DOT_HTML);
        }

        ApiCollector collector = new ApiCollector(environment);
        collector.setDocumentPrivateMembers(documentPrivateMembers);

        Api api = collector.collect(environment.getIncludedElements());
        api.sort();
        LinkResolver.setApi(api);

        MarkdownWriter writer = new MarkdownWriter(outputDirectory);
        try{
            writer.writeDocs(api);
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
            System.err.println(ex.getStackTrace());
            return FAILED;
        }
        return OK;
    }
}