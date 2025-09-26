/// This package contains the classes responsible for writing the Markdown file.
/// [ModuleWriter] handles the top-level module documentation including constant
/// field values for the module. It uses [PackageWriter] to output the Markdown
/// files for each [package][io.github.sandydunlop.cascara.model.PackageNode]
/// contained within, which in turn uses [TypeWriter] to output the Markdown
/// files for each [type][io.github.sandydunlop.cascara.model.TypeNode]
/// contained within.
package io.github.sandydunlop.markista.markdown;
