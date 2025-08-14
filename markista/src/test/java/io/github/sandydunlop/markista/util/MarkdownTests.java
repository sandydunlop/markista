package io.github.sandydunlop.markista.util;

import com.sun.source.util.DocTreePath;

import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ClassTypeNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.Reference;
import io.github.sandydunlop.markista.model.Text;
import io.github.sandydunlop.markista.model.TypeNode;
import io.github.sandydunlop.markista.model.Text.Segment;
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
    private static Context ctx;
	private Api api;
    private ModuleNode module;
    private PackageNode model;
    private PackageNode doclet;
	private PackageNode markista;
	private PackageNode util;
    private ClassTypeNode node;
    private ClassTypeNode markdownDoclet;

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
		ctx = Context.getInstance();
		ctx.setReporter(reporter);
    }

    @BeforeEach
    void init() {
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
		api.addClass(new ClassTypeNode("io.github.sandydunlop.markista.util.LinkResolver","LinkResolver", util));
		api.addClass(new ClassTypeNode("io.github.sandydunlop.markista.doclet.MarkdownDoclet","MarkdownDoclet", doclet));
		api.addClass(new ClassTypeNode("io.github.sandydunlop.markista.doclet.MarkdownDoclet.Option","MarkdownDoclet.Option", doclet));

        module = new ModuleNode("markista");
		api.addModule(module);
		module.addPackage(markista);
		module.addPackage(util);
		module.addPackage(doclet);
		module.addPackage(model);
		markista.setModule(module);
		util.setModule(module);
		doclet.setModule(module);
		model.setModule(module);

        node = new ClassTypeNode("io.github.sandydunlop.markista.model.Node", "Node", model);
        model.addClass(node);
        api.addClass(node);

        markdownDoclet = new ClassTypeNode("io.github.sandydunlop.markista.doclet.MarkdownDoclet", "MarkdownDoclet", doclet);
        model.addClass(markdownDoclet);

        LinkResolver.init(api, ctx);
        LinkResolver.addNativeModuleUrl("java.base", JAVA_24_URL + "java.base", ".html");
		LinkResolver.setFlattenedDirectories(null);
		ctx.setModuleName("markista");
        ctx.setPackageName("io.github.sandydunlop.markista.doclet");
    }

    @Test
    void formatParams() {
        List<ParamNode> params = new ArrayList<>();
        TypeNode param1type = new TypeNode("java.lang.String", "String", model);
        ParamNode param1 = new ParamNode(param1type, "name");
        params.add(param1);

        MethodNode method = new MethodNode(node, "subject");
        method.addParam(param1);
        markdownDoclet.addMethod(method);
        api.addClass(markdownDoclet);
        LinkResolver.init(api, ctx);
        LinkResolver.addNativeModules();
        LinkFormatter.generateLinkTexts(api, ctx);

        String markdown = Markdown.formatParams(params);
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
        String formatted = Markdown.formatText(text);
        assertEquals("hello world", formatted);
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
        LinkFormatter.generateLinkTexts(api, ctx);
        String formatted = Markdown.formatText(text);
        assertEquals("hello [link](http://example.com) world", formatted);
    }

    @Test
    void fullSignature() {
        MethodNode method = new MethodNode(node, "subject");
        TypeNode param1type = new TypeNode("java.lang.String", "String", model);
        method.addParam(new ParamNode(param1type, "name"));
        markdownDoclet.addMethod(method);
        api.addClass(markdownDoclet);
        LinkResolver.init(api, ctx);
        LinkResolver.addNativeModules();
        LinkFormatter.generateLinkTexts(api, ctx);
        String sig = Markdown.fullSignature(method);
        assertEquals("[Node](../model/Node.md) subject([String](" + JAVA_24_URL + "java.base/java/lang/String.html) name)", sig);
    }
}
