package io.github.sandydunlop.markista.util;

import com.sun.source.util.DocTreePath;

import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ClassNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.Reference;
import io.github.sandydunlop.markista.model.TypeNode;
import jdk.javadoc.doclet.Reporter;

import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class MarkdownTests {
    private static final String JAVA_24_URL = "https://docs.oracle.com/en/java/javase/24/docs/api/";
	private Api api;
    private ModuleNode module;
    private PackageNode model;
    private PackageNode doclet;
	private PackageNode markista;
	private PackageNode util;
    private ClassNode node;
    private ClassNode markdownDoclet;

    @Mock static Reporter reporter = new Reporter() {
        @Override
        public void print(Kind kind, String message) {
            System.out.println(kind + ": " + message);
        }

        @Override
        public void print(Kind kind, DocTreePath path, String message) {
            // Do nothing
        }

        @Override
        public void print(Kind kind, Element element, String message) {
            // Do nothing
        }
    };
    
    @BeforeAll
    static void initAll() {
		Configuration.setReporter(reporter);
        LinkResolver.addNativeModule("java.base", "https://docs.oracle.com/en/java/javase/24/docs/api/java.base", ".html");
    }

    @BeforeEach
    void init() {
		api = new Api();
        api.addPackage(new PackageNode("io.github.sandydunlop"));
        markista = new PackageNode("io.github.sandydunlop.markista");
		util = new PackageNode("io.github.sandydunlop.markista.util");
		doclet = new PackageNode("io.github.sandydunlop.markista.doclet");
		model = new PackageNode("io.github.sandydunlop.markista.model");
		api.addPackage(markista);
		api.addPackage(util);
		api.addPackage(doclet);
		api.addPackage(model);
		api.addClass(new ClassNode("io.github.sandydunlop.markista.util.LinkResolver","LinkResolver", util));
		api.addClass(new ClassNode("io.github.sandydunlop.markista.doclet.MarkdownDoclet","MarkdownDoclet", doclet));
		api.addClass(new ClassNode("io.github.sandydunlop.markista.doclet.MarkdownDoclet.Option","MarkdownDoclet.Option", doclet));

        module = new ModuleNode("sandydunlop.markista");
		api.addModule(module);
		module.addPackage(markista);
		module.addPackage(util);
		module.addPackage(doclet);
		module.addPackage(model);
		markista.setModule(module);
		util.setModule(module);
		doclet.setModule(module);
		model.setModule(module);

        node = new ClassNode("io.github.sandydunlop.markista.model.Node", "Node", model);
        model.addClass(node);
        api.addClass(node);

        markdownDoclet = new ClassNode("io.github.sandydunlop.markista.doclet.MarkdownDoclet", "MarkdownDoclet", doclet);
        model.addClass(markdownDoclet);

        LinkResolver.setApi(api);
		LinkResolver.setFlattenedDirectories(null);
		LinkResolver.setCurrentModuleName("sandydunlop.markista");
        LinkResolver.setCurrentPackageName("io.github.sandydunlop.markista.doclet");
    }

    @Test
    void fullSignature() {
        MethodNode method = new MethodNode(node, "subject");
        TypeNode param1type = new TypeNode("java.lang.String", "String", model);
        method.addParam(new ParamNode(param1type, "name"));
        String sig = Markdown.fullSignature(method);
        assertEquals("[Node](../model/Node.md) subject([String](" + JAVA_24_URL + "java.base/java/lang/String.html) name)", sig);
    }

    @Test
    void formatParams() {
        List<ParamNode> params = new ArrayList<>();
        TypeNode param1type = new TypeNode("java.lang.String", "String", model);
        params.add(new ParamNode(param1type, "name"));
        String markdown = Markdown.formatParams(params);
        assertEquals("[String](" + JAVA_24_URL + "java.base/java/lang/String.html) name", markdown);
    }

    @Test
    void formatReference_URL() {
        Reference ref = new Reference(Reference.Kind.URL, "name", "http://example.com");
        String markdown = Markdown.formatReference(ref);
        assertEquals("[http://example.com](http://example.com)", markdown);
    }

    @Test
    void formatReference_PACKAGE_qualified() {
        Reference ref = new Reference(Reference.Kind.PACKAGE, "io.github.sandydunlop.markista.model", null);
        String markdown = Markdown.formatReference(ref);
        assertEquals("[io.github.sandydunlop.markista.model](../model/index.md)", markdown);
    }

    @Test
    void formatReference_PACKAGE_unqualified() {
        Reference ref = new Reference(Reference.Kind.PACKAGE, "model", null);
        String markdown = Markdown.formatReference(ref);
        assertEquals("[model](../model/index.md)", markdown);
    }

    static List<Object[]> typeReferenceProvider() {
        return List.of(
            new Object[] { Reference.Kind.TYPE, "io.github.sandydunlop.model.Node", null, "Node" },
            new Object[] { Reference.Kind.TYPE, "io.github.sandydunlop.markista.model.Node", null, "[Node](../model/Node.md)" },
            new Object[] { Reference.Kind.TYPE, "Node", null, "[Node](../model/Node.md)" }
        );
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("typeReferenceProvider")
    void formatReference_TYPE_variants(Reference.Kind kind, String name, String url, String expected) {
        Reference ref = new Reference(kind, name, url);
        String markdown = Markdown.formatReference(ref);
        assertEquals(expected, markdown);
    }

    @Test
    void formatReference_PAGE_withTitle() {
        Reference ref = new Reference(Reference.Kind.PAGE, "Page", "page");
        String markdown = Markdown.formatReference(ref);
        assertEquals("[Page](../../../../../page.md)", markdown);
    }

    @Test
    void formatReference_PAGE_withoutTitle() {
        Reference ref = new Reference(Reference.Kind.PAGE, "", "page");
        String markdown = Markdown.formatReference(ref);
        assertEquals("[page](../../../../../page.md)", markdown);
    }

    @Test
    void formatReference_PRIMITIVE() {
        // This should never be created, so its value here shouldn't matter
        Reference ref = new Reference(Reference.Kind.PRIMITIVE, "int", null);
        String markdown = Markdown.formatReference(ref);
        assertEquals("", markdown);
    }

	@Test
	void link_qualifiedWithAnchor() {
        LinkResolver.setCurrentPackageName("io.github.sandydunlop.markista.doclet.MarkdownDoclet");
        String markdown = Markdown.mdAutoLink("io.github.sandydunlop.markista.util.Markdown#mdAutoLink", false);
        assertEquals("[mdautolink](../util/Markdown.md#mdautolink)", markdown);
    }

	@Test
	void autoLink_array() {
        String markdown = Markdown.mdAutoLink("java.lang.String[]", true);
        assertEquals("[String](" + JAVA_24_URL + "java.base/java/lang/String.html)[]", markdown);
    }

    @Test
	void autoLink_listOfArrays() {
        String markdown = Markdown.mdAutoLink("java.util.List<java.lang.String[]>", true);
        assertEquals("[List](" + JAVA_24_URL + "java.base/java/util/List.html)&lt;[String](" + JAVA_24_URL + "java.base/java/lang/String.html)[]&gt;", markdown);
    }

	@Test
	void splitAndLink_oneArray() {
        String markdown = Markdown.splitAndLink("java.lang.String[]");
        assertEquals("[String](" + JAVA_24_URL + "java.base/java/lang/String.html)[]", markdown);
    }

	@Test
	void splitAndLink_two() {
        String markdown = Markdown.splitAndLink("java.lang.String, java.util.List");
        assertEquals("[String](" + JAVA_24_URL + "java.base/java/lang/String.html), [List](" + JAVA_24_URL + "java.base/java/util/List.html)", markdown);
    }
}
