package io.github.sandydunlop.markista.markdown;

import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ClassNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.Link;
import io.github.sandydunlop.markista.model.Text;
import io.github.sandydunlop.markista.model.Text.Segment;
import io.github.sandydunlop.markista.orchestration.LinkResolver;
import io.github.sandydunlop.markista.orchestration.TextAssembler;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;

import jdk.javadoc.doclet.Reporter;

import com.sun.source.util.DocTreePath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownTests {
    private static final String JAVA_24_URL = "https://docs.oracle.com/en/java/javase/24/docs/api/";
    private static Context ctx;
	private Api api;
    private ModuleNode module;
    private PackageNode model;
    private PackageNode doclet;
	private PackageNode markista;
	private PackageNode util;
    private ClassNode node;
    private ClassNode markdownDoclet;

    static TestReporter reporter;

    @AfterEach
    void resetSingleton() {
        Context.reset();
    }

    @BeforeAll
    static void initAll() {
        Context.reset();
		ctx = Context.getInstance();
    }

    @BeforeEach
    void init() {
        reporter = new TestReporter();
        reporter.stringWriter = new StringWriter();
        ctx.setReporter(reporter);

        api = new Api("Test API");
        api.addPackage(new PackageNode("io.github.sandydunlop"));
        markista = new PackageNode("io.github.sandydunlop.markista");
		util = new PackageNode("io.github.sandydunlop.markista.util");
		doclet = new PackageNode("io.github.sandydunlop.markista.doclet");
		model = new PackageNode("io.github.sandydunlop.markista.model");
		api.addPackage(markista);
		api.addPackage(util);
		api.addPackage(doclet);
		api.addPackage(model);
		api.addType(new ClassNode("LinkResolver", util.getName()));
		api.addType(new ClassNode("MarkdownDoclet", doclet.getName()));
		api.addType(new ClassNode("MarkdownDoclet.Option", doclet.getName()));

        module = new ModuleNode("markista");
		api.addModule(module);
		module.addPackage(markista);
		module.addPackage(util);
		module.addPackage(doclet);
		module.addPackage(model);
		markista.setModuleName(module.getName());
		util.setModuleName(module.getName());
		doclet.setModuleName(module.getName());
		model.setModuleName(module.getName());

        node = new ClassNode("Node", model.getName());
        model.addType(node);
        api.addType(node);

        markdownDoclet = new ClassNode("MarkdownDoclet", doclet.getName());
        model.addType(markdownDoclet);

        LinkResolver.init(api, ctx);
        LinkResolver.addStandardModuleUrl("java.base", JAVA_24_URL + "java.base", ".html");
		LinkResolver.setFlattenedDirectories(null);
		ctx.setModuleName("markista");
        ctx.setPackageName("io.github.sandydunlop.markista.doclet");
    }

    @Test
    void formatParams() {
        List<ParamNode> params = new ArrayList<>();
        ParamNode param1 = new ParamNode("java.lang.String", "name");
        params.add(param1);

        MethodNode method = new MethodNode(node.getQualifiedName(), "subject");
        method.addParam(param1);
        markdownDoclet.addMethod(method);
        api.addType(markdownDoclet);
        LinkResolver.init(api, ctx);
        LinkResolver.addStandardModules();
        TextAssembler.assembleTextAndLinks(api, ctx);

        String markdown = MarkdownUtils.formatParams(params);
        assertEquals("[String](" + JAVA_24_URL + "java.base/java/lang/String.html) name", markdown);
    }


    static List<Object[]> typeReferenceProvider() {
        return List.of(
            new Object[] { Link.Kind.TYPE, "io.github.sandydunlop.Node", null, "io.github.sandydunlop.Node" },
            new Object[] { Link.Kind.TYPE, "io.github.sandydunlop.markista.model.Node", null, "[Node](../model/Node.md)" },
            new Object[] { Link.Kind.TYPE, "Node", null, "[Node](../model/Node.md)" }
        );
    }

    @Test
    void formatText_text() {
        Text text = Text.empty();
        text.append(Segment.empty()
                .setKind(Text.Segment.Kind.TEXT)
                .setText("hello world"));
        String formatted = MarkdownUtils.formatText(text);
        assertEquals("hello world", formatted);
    }

    @Test
    void formatText_code() {
        Text text = Text.empty();
        text.append(Segment.empty()
                .setKind(Text.Segment.Kind.CODE)
                .setText("#!/bin/zsh"));
        String formatted = MarkdownUtils.formatText(text);
        assertEquals("`#!/bin/zsh`", formatted);
    }

    @Disabled("INVESTIGATING")
    @Test
    void formatText_unhandled() {
        Text text = Text.empty();
        text.append(Segment.empty().setText("unhandled").setKind(Text.Segment.Kind.NONE));
        reporter.stringWriter = new StringWriter();
        String formatted = MarkdownUtils.formatText(text);
        assertTrue(reporter.stringWriter.toString().contains("nhandled javadoc tag"));
        assertEquals("", formatted);
    }

    @Test
    void formatText_text_link_text() {
        Link link = Link.to("http://example.com");
        Text text = Text.empty();
        text.append(Segment.empty()
                .setKind(Text.Segment.Kind.TEXT)
                .setText("hello "));
        text.append(Segment.empty()
                .setKind(Text.Segment.Kind.LINK)
                .setText("link")
                .setLink(link));
        text.append(Segment.empty()
                .setKind(Text.Segment.Kind.TEXT)
                .setText(" world"));
        api.addLink(link);
        LinkResolver.init(api, ctx);
        TextAssembler.assembleTextAndLinks(api, ctx);
        String formatted = MarkdownUtils.formatText(text);
        assertEquals("hello [link](http://example.com) world", formatted);
    }

    @Test
    void fullSignature() {
        MethodNode method = new MethodNode(node.getQualifiedName(), "subject");
        method.addParam(new ParamNode("java.lang.String", "name"));
        markdownDoclet.addMethod(method);
        api.addType(markdownDoclet);
        LinkResolver.init(api, ctx);
        LinkResolver.addStandardModules();
        TextAssembler.assembleTextAndLinks(api, ctx);
        String sig = MarkdownUtils.fullSignature(method);
        assertEquals("[Node](../model/Node.md) subject([String](" + JAVA_24_URL + "java.base/java/lang/String.html) name)", sig);
    }

    @Test
    void mdDocumentLink_doc() {
        String md = MarkdownUtils.mdDocumentLink("page");
        assertEquals("[page](page.md)", md);
    }

    @Test
    void mdDocumentLink_doc2() {
        String md = MarkdownUtils.mdDocumentLink("page.md");
        assertEquals("[page.md](page.md)", md);
    }

    @Test
    void mdDocumentLink_url() {
        String md = MarkdownUtils.mdDocumentLink("https://example.com");
        assertEquals("[https://example.com](https://example.com)", md);
    }

    class TestReporter implements Reporter {
        public StringWriter stringWriter;

        @Override
        public void print(javax.tools.Diagnostic.Kind kind, String message) {
            stringWriter.write(message);
        }

        @Override
        public void print(javax.tools.Diagnostic.Kind kind, DocTreePath path, String message) {
            stringWriter.write(message);
        }

        @Override
        public void print(javax.tools.Diagnostic.Kind kind, Element element, String message) {
            stringWriter.write(message);
        }
    }
}
