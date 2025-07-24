/// Markista is a tool for generating Markdown documentation from Java source code.
/// It is designed to work with Java 11 and later versions.
/// The module-info.java file defines the module and its exports.
/// It exports the doclet, model, and util packages for use by other modules.
module sandydunlop.markista {
    requires jdk.compiler;
    requires transitive jdk.javadoc;
    requires transitive java.compiler;

    exports io.github.sandydunlop.markista.doclet;
    exports io.github.sandydunlop.markista.model;
    exports io.github.sandydunlop.markista.util;
}
