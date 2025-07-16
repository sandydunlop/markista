# Markista

The Markdown Doclet


## Introduction

Markist is a [doclet](https://docs.oracle.com/javase/8/docs/technotes/guides/javadoc/doclet/overview.html) that turns your Java documentation comments into Markdown, suitable for use with [Github](https://github.com/), [MkDocs](https://www.mkdocs.org/), etc. 

Marklist is under active development and has reached a point where it is becoming useful. There will be features that aren't completely implemented yet.

As a demonstration, the Markdown Javadoc for Markista itself is [available here](https://sandydunlop.github.io/markista/javadoc/doclet/MarkdownDoclet/).
It was produced with the `--private` parameter which tells Markista to document private members of classes. By default only public and protected members are documented.

## Gradle

Markista can be used from a [Gradle](https://gradle.org/) build by adding the following code to your Gradle build file.
Change `libs/markista-0.1.0.jar` to match wherever you've put the Markista JAR file.

```groovy
javadoc {
    options {
        source = sourceSets.main.allJava
        destinationDir = file("$buildDir/docs/javadoc")
        options.doclet = 'io.github.sandydunlop.markista.doclet.MarkdownDoclet'
        options.docletpath = files('libs/markista-0.1.0.jar')
        options.encoding = 'UTF-8'
        options.source = null
    }
}
```

## Command Line

Markista can be used from the command line with the `javadoc` command as follows:

```bash
javadoc -docletpath libs/markista-0.1.0.jar -doclet io.github.sandydunlop.markista.doclet.MarkdownDoclet src/main/java/my.package/Hello.java
```

### Parameters

`-d <directory>`
:   The directory to write the Markdown files to.

`--private`
:   Generate docs for private members. By default only public 
    and abstract members are documented.

`--external-links`
:  Create links to classes defined outside of the API being 
   documented (eg. [java.utils.String](https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/lang/String.html))

!!! note
    Currently only packages within the java.base module are considered
    when linking to classes defined in external packages. It is planned that
    all built in Java packages will be supported, as well as links to other
    projects' documentation.

## Source Code

Markista's souce code is available on [Github](https://github.com/sandydunlop/markista) under the GPLv3 license.
