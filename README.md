# Markista

*Serving Markdown Flavored Documentation*

## Introduction

Markista is a [doclet](https://docs.oracle.com/javase/8/docs/technotes/guides/javadoc/doclet/overview.html) that turns your Java documentation comments into Markdown, suitable for use with [Github](https://github.com/), [MkDocs](https://www.mkdocs.org/), etc. 

> Markista is a strong addition to the list of doclets for generating Markdown documentation from Java source code. Its focus on simplicity and readability makes it a great choice for developers looking to enhance their documentation process. If you're exploring options for Markdown generation, Markista is definitely worth considering alongside other tools like MDoclet, mddoclet, and Doxygen.

## Benefits of using Markista

* Simplicity: Markista is straightforward to use, making it accessible for developers who may not have extensive experience with documentation tools.
* Readable Output: The generated Markdown is clean and easy to read, which is essential for effective documentation.
* Automation: By integrating with build tools, Markista allows for automatic documentation updates, ensuring that the documentation stays in sync with the codebase.
* Extensibility: Markista uses Java [Service Provider Interfaces](https://sandydunlop.github.io/markista/javadoc/markista/spi/DocService/) (SPI) to allow extensions to be written which can take advantage of Markista's easy to use API model.

## Demonstration and Sample Projects
As a demonstration, the Markdown Javadoc for Markista itself is [available here](https://sandydunlop.github.io/markista/Markista-Javadoc/markista/).
It was produced with the `-private` parameter which tells Markista to document private members of classes. By default only public and protected members are documented.

Several [sample projects](https://sandydunlop.github.io/markista/examples/) have been created to show how to integrate Markista with your build.

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
    markista("io.github.sandydunlop:markista:0.1.20")
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

## Maven

pom.xml:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>io.github.sandydunlop</groupId>
    <artifactId>example.module.maven</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>24</maven.compiler.source>
        <maven.compiler.target>24</maven.compiler.target>
        <maven.compiler.release>24</maven.compiler.release>
		<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <repositories>
        <repository>
            <id>maven-local</id>
            <url>file://${user.home}/.m2/repository</url>
        </repository>
        <repository>
            <id>central</id>
            <url>https://repo.maven.apache.org/maven2</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>io.github.sandydunlop</groupId>
            <artifactId>markista</artifactId>
            <version>0.1.20</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>${maven.compiler.source}</source>
                    <target>${maven.compiler.target}</target>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-javadoc-plugin</artifactId>
                <version>3.3.1</version>
                <configuration>
                    <source>${maven.compiler.source}</source>
                    <windowTitle>${project.name} API Documentation</windowTitle>
                    <docTitle>${project.name} API Documentation</docTitle>
                    <additionalparam>-doclet io.github.sandydunlop.markista.doclet.MarkdownDoclet</additionalparam>
                    <additionalparam>-private</additionalparam>
                    <additionalparam>-link</additionalparam>
                    <additionalparam>-verbose</additionalparam>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>

```

## Command Line

Markista can be used from the command line with the `javadoc` command as follows:

```bash
javadoc -docletpath libs/markista-0.1.20.jar -doclet io.github.sandydunlop.markista.doclet.MarkdownDoclet src/main/java/my.package/Hello.java
```

### Parameters

`-d <directory>`
:  The directory where documentation will be written to.

`-d <title>`
:  The title of the API being documented.

`--extensions` <extensions>
:  Colon-seaparated ordered list of extensions to run. If extensions are available through SPI and this option is not specified, they will all be run.

`--flatten-modules`
:  Don't create individual module directories
:  This option should be used if you have multiple modules in individual subprojects and want to be able to link between them in the docs

`--flatten-packages`
:  Don't create package directories that contain no classes.

`-link`
:  Create links to classes defined outside of the API being 
   documented (eg. [java.utils.String](https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/lang/String.html)).

`--link-modules`
:  Create links to modules outside of the API that are on the module-path.

`--module-path`
:  Colon-separated list that specifies where to find application modules.

`-private`
:   Generate docs for private members. By default only public 
    and abstract members are documented.

`-tabs`
:  Display summary tables in content tabs.

`-verbose`
:  Display progress information.

## Download

JAR files for version 0.1.20 are available to [download here](https://github.com/sandydunlop/markista/releases/tag/r0.1.20).


## Source Code

Markista's source code is available on [Github](https://github.com/sandydunlop/markista) under the GPLv3 license.

---

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=sandydunlop_markista&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=sandydunlop_markista)

