package io.github.sandydunlop.markista.util;

import com.sun.source.util.DocTreePath;

import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.Reference;
import io.github.sandydunlop.markista.model.ClassNode;
import io.github.sandydunlop.markista.model.ModuleNode;

import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;

import jdk.javadoc.doclet.Reporter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class LinkResolverTests {
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
        LinkResolver.setCurrentPackageName("");
	}

	@Test
	void resolve_qualifiedNativeClass() {
		Reference link = LinkResolver.resolve("java.util.List");
		assertEquals("https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/util/List.html", link.getUri());
	}

	@Test
	void resolve_unqualifiedPackage_nextLevelPackage() {
		Reference link = LinkResolver.resolve("io.github.sandydunlop.markista", "doclet");
		assertEquals("doclet", link.getUri());
	}

	@Test
	void resolve_unqualifiedPackage_sameLevelPackage() {
		Reference link = LinkResolver.resolve("io.github.sandydunlop.markista.util", "doclet");
		assertEquals("../doclet", link.getUri());
	}

	@Test
	void resolve_unqualifiedClass_sameLevel() {
		Reference link = LinkResolver.resolve("io.github.sandydunlop.markista.util", "MarkdownDoclet");
		assertEquals("../doclet/MarkdownDoclet", link.getUri());
	}

	@Test
	void resolve_qualifiedClass_noLevel() {
		Reference link = LinkResolver.resolve("io.github.sandydunlop.markista.model.Node");
		assertEquals("io/github/sandydunlop/markista/model/Node", link.getUri());
	}

	@Test
	void resolve_unqualifiedPackage_prevLevel() {
		Reference link = LinkResolver.resolve("io.github.sandydunlop.markista.util", "markista");
		assertEquals("..", link.getUri());
	}

	@Test
	void resolve_qualifiedPackage_prevLevel() {
		Reference link = LinkResolver.resolve("io.github.sandydunlop.markista.util", "io.github.sandydunlop.markista");
		assertEquals("..", link.getUri());
	}

	@Test
	void resolve_primitive() {
		Reference link = LinkResolver.resolve("io.github.sandydunlop.markista.util", "boolean");
		assertEquals(Reference.Kind.PRIMITIVE, link.getKind());
	}

	@Test
	void relativize_toParentLevel() {
		String link = LinkResolver.relativize("io.github.sandydunlop.markista.util", "io.github.sandydunlop.markista");
		assertEquals("..", link);
	}

	@Test
	void relativize_toSameLevel() {
		String link = LinkResolver.relativize("io.github.sandydunlop.markista.util", "io.github.sandydunlop.markista.doclet");
		assertEquals("../doclet", link);
	}

	@Test
	void relativize_fromNoLevel() {
		String path = LinkResolver.relativize("", "io.github.sandydunlop.markista.model.Node");
		assertEquals("io/github/sandydunlop/markista/model/Node", path);
	}

	@Test
	void relativize_squashed_toSameLevel() {
		LinkResolver.setFlattenedDirectories("io.github.sandydunlop.markista");
		String path = LinkResolver.relativize("io.github.sandydunlop.markista.util", "io.github.sandydunlop.markista.doclet");
		assertEquals("../doclet", path);
	}

	@Test
	void relativize_squashed_fromNoLevel() {
		LinkResolver.setFlattenedDirectories("io.github.sandydunlop.markista");
		String path = LinkResolver.relativize("", "io.github.sandydunlop.markista.doclet");
		assertEquals("doclet", path);
	}
}
