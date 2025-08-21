package io.github.sandydunlop.markista.markdown;

import io.github.sandydunlop.markista.assembler.LinkResolver;
import io.github.sandydunlop.markista.assembler.TextAssembler;
import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ClassTypeNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.Reference;
import io.github.sandydunlop.markista.model.Text;
import io.github.sandydunlop.markista.model.Text.Segment;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;

import jdk.javadoc.doclet.Reporter;

import com.sun.source.util.DocTreePath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class MarkdownTests {
    private static final String JAVA_24_URL = "https://docs.oracle.com/en/java/javase/24/docs/api/";
    private static Context ctx;
	private Api api;
    private ModuleNode module;
    private PackageNode model;
    private PackageNode doclet;
	private PackageNode markista;
	private PackageNode util;
    private ClassTypeNode node;
    private ClassTypeNode markdownDoclet;

    static TestReporter reporter;
    
	@BeforeAll
    static void initAll() {
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
		api.addType(new ClassTypeNode("io.github.sandydunlop.markista.util.LinkResolver",
                "LinkResolver", util.getQualifiedName()));
		api.addType(new ClassTypeNode("io.github.sandydunlop.markista.doclet.MarkdownDoclet",
                "MarkdownDoclet", doclet.getQualifiedName()));
		api.addType(new ClassTypeNode("io.github.sandydunlop.markista.doclet.MarkdownDoclet.Option",
                "MarkdownDoclet.Option", doclet.getQualifiedName()));

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

        node = new ClassTypeNode("io.github.sandydunlop.markista.model.Node", 
                "Node", model.getQualifiedName());
        model.addType(node);
        api.addType(node);

        markdownDoclet = new ClassTypeNode("io.github.sandydunlop.markista.doclet.MarkdownDoclet", 
                "MarkdownDoclet", doclet.getQualifiedName());
        model.addType(markdownDoclet);

        LinkResolver.init(api, ctx);
        LinkResolver.addNativeModuleUrl("java.base", JAVA_24_URL + "java.base", ".html");
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
        LinkResolver.addNativeModules();
        TextAssembler.assembleTextAndLinks(api, ctx);

        String markdown = MarkdownUtils.formatParams(params);
        assertEquals("[String](" + JAVA_24_URL + "java.base/java/lang/String.html) name", markdown);
    }


    static List<Object[]> typeReferenceProvider() {
        return List.of(
            new Object[] { Reference.Kind.TYPE, "io.github.sandydunlop.Node", null, "io.github.sandydunlop.Node" },
            new Object[] { Reference.Kind.TYPE, "io.github.sandydunlop.markista.model.Node", null, "[Node](../model/Node.md)" },
            new Object[] { Reference.Kind.TYPE, "Node", null, "[Node](../model/Node.md)" }
        );
    }

    @Test
    void formatText_text() {
        Text text = Text.empty();
        text.append(Segment.empty()
                .setKind(Text.SegmentKind.TEXT)
                .setText("hello world"));
        String formatted = MarkdownUtils.formatText(text);
        assertEquals("hello world", formatted);
    }

    @Test
    void formatText_code() {
        Text text = Text.empty();
        text.append(Segment.empty()
                .setKind(Text.SegmentKind.CODE)
                .setText("#!/bin/zsh"));
        String formatted = MarkdownUtils.formatText(text);
        assertEquals("`#!/bin/zsh`", formatted);
    }

    @Test
    void formatText_unhandled() {
        Text text = Text.empty();
        text.append(Segment.empty().setText("unhandled").setKind(Text.SegmentKind.NONE));
        reporter.stringWriter = new StringWriter();
        String formatted = MarkdownUtils.formatText(text);
        assertTrue(reporter.stringWriter.toString().contains("nhandled javadoc tag"));
        assertEquals("", formatted);
    }

    @Test
    void formatText_text_link_text() {
        Reference link = Reference.to("http://example.com");
        Text text = Text.empty();
        text.append(Segment.empty()
                .setKind(Text.SegmentKind.TEXT)
                .setText("hello "));
        text.append(Segment.empty()
                .setKind(Text.SegmentKind.LINK)
                .setText("link")
                .setLink(link));
        text.append(Segment.empty()
                .setKind(Text.SegmentKind.TEXT)
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
        LinkResolver.addNativeModules();
        TextAssembler.assembleTextAndLinks(api, ctx);
        String sig = MarkdownUtils.fullSignature(method);
        assertEquals("[Node](../model/Node.md) subject([String](" + JAVA_24_URL + "java.base/java/lang/String.html) name)", sig);
    }

    @Test
    void setDisplayName_1() {
        Reference link = Reference.to("io.github.sandydunlop.markista.model.Node")
                .from("io.github.sandydunlop.markista.model")
                .withLabel("Node")
                .withKind(Reference.Kind.TYPE);
        link.setUri("Node.md");
        String displayName = "";
        boolean isLocalMethod = false;
        boolean useQualifiedName = false;
        MarkdownUtils.setContext(ctx);
        MarkdownUtils.setDisplayName(link, displayName, isLocalMethod, useQualifiedName);
        assertEquals("Node.md", link.getUri());
        assertEquals("Node", link.getLabel());
    }

    @Test
    void setDisplayName_2() {
        Reference link = Reference.to("io.github.sandydunlop.markista.model.Node")
                .from("io.github.sandydunlop.markista.model")
                .withLabel("Node")
                .withKind(Reference.Kind.TYPE);
        link.setUri("Node.md");
        String displayName = "Display";
        boolean isLocalMethod = false;
        boolean useQualifiedName = false;
        MarkdownUtils.setContext(ctx);
        MarkdownUtils.setDisplayName(link, displayName, isLocalMethod, useQualifiedName);
        assertEquals("Node.md", link.getUri());
        assertEquals("Display", link.getLabel());
    }

    @Test
    void setDisplayName_3() {
        Reference link = Reference.to("Node.test")
                .from("io.github.sandydunlop.markista.model")
                .withLabel("Node")
                .withKind(Reference.Kind.METHOD);
        link.setUri("Node.md");
        link.setClassName("Node");
        link.setAnchor("#test");
        String displayName = "";
        boolean isLocalMethod = true;
        boolean useQualifiedName = false;
        ctx.setTypeName("Node");
        MarkdownUtils.setContext(ctx);
        MarkdownUtils.setDisplayName(link, displayName, isLocalMethod, useQualifiedName);
        assertEquals("Node.md", link.getUri());
        assertEquals("test", link.getLabel());
    }

    @Test
    void setDisplayName_4() {
        Reference link = Reference.to("Node.test")
                .from("io.github.sandydunlop.markista.model")
                .withLabel("Node")
                .withKind(Reference.Kind.METHOD);
        link.setUri("Node.md");
        link.setClassName("Node");
        link.setAnchor("#test");
        String displayName = "";
        boolean isLocalMethod = true;
        boolean useQualifiedName = false;
        ctx.setTypeName("Api");
        MarkdownUtils.setContext(ctx);
        MarkdownUtils.setDisplayName(link, displayName, isLocalMethod, useQualifiedName);
        assertEquals("Node.md", link.getUri());
        assertEquals("Node.test", link.getLabel());
    }

    @Test
    void setDisplayName_5() {
        Reference link = Reference.to("Node")
                .from("io.github.sandydunlop.markista.model")
                .withKind(Reference.Kind.METHOD)
                .withLabel(null);
        link.setUri("Node.md");
        String displayName = "";
        boolean isLocalMethod = false;
        boolean useQualifiedName = false;
        ctx.setTypeName("Api");
        MarkdownUtils.setContext(ctx);
        MarkdownUtils.setDisplayName(link, displayName, isLocalMethod, useQualifiedName);
        assertEquals("Node.md", link.getUri());
        assertEquals("Node", link.getLabel());
    }

    @Test
    void setDisplayName_7() {
        Reference link = Reference.to("markista.docagrams")
                .from("")
                .withKind(Reference.Kind.MODULE);
        link.setUri("markista.docagrams/index.md");
        String displayName = "";
        boolean isLocalMethod = false;
        boolean useQualifiedName = false;
        ctx.setTypeName("Api");
        MarkdownUtils.setContext(ctx);
        MarkdownUtils.setDisplayName(link, displayName, isLocalMethod, useQualifiedName);
        assertEquals("markista.docagrams/index.md", link.getUri());
        assertEquals("markista.docagrams", link.getLabel());
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
