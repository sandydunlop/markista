# Markista

*Serving Markdown Flavored Documentation*

## Introduction

Markista is a [doclet](https://docs.oracle.com/javase/8/docs/technotes/guides/javadoc/doclet/overview.html) that turns your Java documentation comments into Markdown, suitable for use with [Github](https://github.com/), [MkDocs](https://www.mkdocs.org/), etc. 

Markista is under active development and has reached a point where it is becoming useful. There will be features that aren't completely implemented yet.

As a demonstration, the Markdown Javadoc for Markista itself is [available here](https://sandydunlop.github.io/markista/javadoc/doclet/).
It was produced with the `-private` parameter which tells Markista to document private members of classes. By default only public and protected members are documented.

## Gradle

Markista can be used from a [Gradle](https://gradle.org/) build by following these simple steps:

### Step 1: Define a Configuration for the Doclet

Create a configuration in your build.gradle file to manage the Markista dependency.

```groovy
configurations {
    markista
}
```

### Step 2: Add the Markista Dependency

Add Markista as a dependency under the configuration you just created.
Markista is available in the [Maven Central](https://central.sonatype.com/artifact/io.github.sandydunlop/markista) repository.

```groovy
dependencies {
    markista("io.github.sandydunlop:markista:0.1.12")
}
```

### Step 3: Configure the Javadoc Task

In the Javadoc task, specify the doclet path and the doclet class. This tells Gradle where to find the doclet and which one to use.

```groovy
javadoc {
    options {
        docletpath = configurations.markista.files.asType(List)
        doclet = 'io.github.sandydunlop.markista.doclet.MarkdownDoclet'
        source = null
        addBooleanOption('-flatten-packages', true)
        addBooleanOption('link', true)
    }
}
```

## Command Line

Markista can be used from the command line with the `javadoc` command as follows:

```bash
javadoc -docletpath libs/markista-0.1.12.jar -doclet io.github.sandydunlop.markista.doclet.MarkdownDoclet src/main/java/my.package/Hello.java
```

### Parameters

`-d <directory>`
:   The directory to write the Markdown files to.

`-private`
:   Generate docs for private members. By default only public 
    and abstract members are documented.

`--flatten-modules`
:  Don't create individual module directories
:  This option should be used if you have multiple modules in individual subprojects and want to be able to link between them in the docs

`--flatten-packages`
:  Don't create package directories that contain no classes

`-link`
:  Create links to classes defined outside of the API being 
   documented (eg. [java.utils.String](https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/lang/String.html))

`--link-modules`
:  Create links to modules outside of the API that are on the module-path

`-tabs`
:  Display summary tables in content tabs

`-verbose`
:  Display progress information

## Download

JAR files for version 0.1.12 are available to [download here](https://github.com/sandydunlop/markista/releases/tag/r0.1.12).


## Source Code

Markista's source code is available on [Github](https://github.com/sandydunlop/markista) under the GPLv3 license.

---

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=sandydunlop_markista&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=sandydunlop_markista)

