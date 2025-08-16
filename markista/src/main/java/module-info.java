import io.github.sandydunlop.markista.spi.DocService;

/// Markista is a tool for generating Markdown documentation from Java source code.
/// It is designed to work with Java 24 and later versions.
/// The module-info.java file defines the module and its exports.
module markista {
    uses DocService;
    
    requires jdk.compiler;
    requires transitive jdk.javadoc;
    requires transitive java.compiler;

    exports io.github.sandydunlop.markista.core;
    exports io.github.sandydunlop.markista.doclet;
    exports io.github.sandydunlop.markista.model;
    exports io.github.sandydunlop.markista.spi;

    // These opens are needed to allow JUnit testing
    opens io.github.sandydunlop.markista.core;
    opens io.github.sandydunlop.markista.doclet;
    opens io.github.sandydunlop.markista.markdown;
    opens io.github.sandydunlop.markista.model;
    opens io.github.sandydunlop.markista.spi;
    opens io.github.sandydunlop.markista.util;
}
