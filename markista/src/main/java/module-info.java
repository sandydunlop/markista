/// Markista is a tool for generating Markdown documentation from Java source code.
/// It is designed to work with Java 24 and later versions.
/// The module-info.java file defines the module and its exports.
/// It exports only the doclet package for external use. 
module markista {
    requires jdk.compiler;
    requires transitive jdk.javadoc;
    requires transitive java.compiler;

    exports io.github.sandydunlop.markista.doclet;

    // These opens are needed to allow JUnit testing
    opens io.github.sandydunlop.markista.doclet;
    opens io.github.sandydunlop.markista.markdown;
    opens io.github.sandydunlop.markista.model;
    opens io.github.sandydunlop.markista.structure;
    opens io.github.sandydunlop.markista.util;
}
