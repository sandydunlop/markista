package io.github.sandydunlop.markista.doclet;

import io.github.sandydunlop.markista.MockedDocletEnvironment;
import io.github.sandydunlop.markista.core.Configuration;
import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.doclet.MarkdownDoclet.Option;
import io.github.sandydunlop.markista.markdown.MarkdownService;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.spi.DocService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import javax.lang.model.element.Element;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarkdownDocletTests extends MockedDocletEnvironment {
    @Test
    void mocks() {
        mockDocletEnvironment();
        assertNotNull(elementUtilsMock);
        assertNotNull(typeUtilsMock);
        assertNotNull(treeUtilsMock);
        assertNotNull(docletEnvironmentMock.getIncludedElements());
    }

    @Test
    void run_withoutElements() {
        mockDocletEnvironment();
        // Configuration.setCreateExternalLinks(true);
        List<Element> elements = new ArrayList<>();
        mockIncludedElements(elements);
        doclet.run(docletEnvironmentMock);
        assertNotNull(ctx.getReporter());
        assertNotNull(ctx.getApi());
    }

    @Test
    void extensions_withoutOrder() {
        mockDocletEnvironment();
        DocService service = mock(TestDocService.class);
        @SuppressWarnings("unchecked")
        ServiceLoader<DocService> loader = mock(ServiceLoader.class);
        when(loader.iterator()).thenReturn(Collections.singletonList(service).iterator());

        DocService defaultDocService = new MarkdownService();
        List<DocService> orderedExtensions = new ArrayList<>();

        System.out.println(Configuration.getExtensionsOrder());
        doclet.getMainServiceAndExtensions(loader, defaultDocService, orderedExtensions);

        verify(service, times(1)).replacesDefault();
    }

    @Test
    void extensions_withOrder() {
        mockDocletEnvironment();
        Configuration.setExtensionsOrder("MarkdownDocletTests$TestDocService");
        DocService service = mock(TestDocService.class);
        @SuppressWarnings("unchecked")
        ServiceLoader<DocService> loader = mock(ServiceLoader.class);
        when(loader.iterator()).thenReturn(Collections.singletonList(service).iterator());

        DocService defaultDocService = new MarkdownService();
        List<DocService> orderedExtensions = new ArrayList<>();

        DocService mainDocService = doclet.getMainServiceAndExtensions(loader, defaultDocService, orderedExtensions);

        assertNotNull(mainDocService);
        verify(service, times(1)).replacesDefault();
    }

    @Test
    void extensions_multipleReplacements() {
        mockDocletEnvironment();
        DocService service1 = mock(TestDocService.class);
        when(service1.replacesDefault()).thenReturn(true);
        DocService service2 = mock(TestDocService2.class);
        when(service2.replacesDefault()).thenReturn(true);
        @SuppressWarnings("unchecked")
        ServiceLoader<DocService> loader = mock(ServiceLoader.class);
        when(loader.iterator()).thenReturn(List.of(service1, service2).iterator());

        DocService defaultDocService = new MarkdownService();
        List<DocService> orderedExtensions = new ArrayList<>();

        DocService mainDocService = doclet.getMainServiceAndExtensions(loader, defaultDocService, orderedExtensions);

        assertNull(mainDocService);
        verify(service1, times(1)).replacesDefault();
        verify(service2, times(1)).replacesDefault();
    }

    @Test
    void options_string() {
        mockDocletEnvironment();
        Set<? extends Option> options = doclet.getSupportedOptions();
        for (Option option : options) {
            String description = option.getDescription();
            assertNotNull(description);
            assertNotEquals("", description);
            switch (option.getNames().getFirst()) {
                case "-d":
                    assertTrue(option.process("-d", List.of("build/docs/javadoc")));
                    assertEquals("build/docs/javadoc", ctx.getOutputDirectory());
                    break;
                case "-doctitle":
                    assertTrue(option.process("-doctitle", List.of("Test API")));
                    assertEquals("Test API", Configuration.getDocTitle());
                    break;
                case "--extensions":
                    assertTrue(option.process("--extensions", List.of("UMLWriter:Docagrams")));
                    assertEquals("UMLWriter:Docagrams", Configuration.getExtensionsOrder());
                    break;
                case "--link-modules":
                    assertTrue(option.process("--link-modules", List.of("module1:module2")));
                    assertEquals("module1:module2", Configuration.getAddModules());
                    break;
                case "--module-path":
                    assertTrue(option.process("--module-path", List.of("module1:module2")));
                    assertEquals("module1:module2", Configuration.getModulePaths());
                    break;
                case "--project-path":
                    assertTrue(option.process("--project-path", List.of("/home/git/project")));
                    assertEquals("/home/git/project", Configuration.getProjectPath());
                    break;
                default:
                    break;
            }
        }
    }

    @Test
    void options_boolean() {
        mockDocletEnvironment();
        Set<? extends Option> options = doclet.getSupportedOptions();
        for (Option option : options) {
            String description = option.getDescription();
            assertNotNull(description);
            assertNotEquals("", description);
            switch (option.getNames().getFirst()) {
                case "--flatten-modules":
                    assertTrue(option.process("--flatten-modules", null));
                    assertTrue(Configuration.getFlattenModules());
                    break;
                case "--flatten-packages":
                    assertTrue(option.process("--flatten-packages", null));
                    assertTrue(Configuration.getFlattenPackages());
                    break;
                // case "-link":
                //     assertTrue(option.process("-link", null));
                //     assertTrue(Configuration.getCreateExternalLinks());
                //     break;
                case "-private":
                    assertTrue(option.process("-private", null));
                    assertTrue(Configuration.getDocumentPrivateMembers());
                    break;
                case "-tabs":
                    assertTrue(option.process("-tabs", null));
                    assertTrue(Configuration.getUseContentTabs());
                    break;
                case "-verbose":
                    assertTrue(option.process("-verbose", null));
                    assertTrue(Configuration.getVerbose());
                    break;
                default:
                    break;
            }
        }
    }

    @Test
    void options_unused() {
        mockDocletEnvironment();
        Set<? extends Option> options = doclet.getSupportedOptions();
        for (Option option : options) {
            String description = option.getDescription();
            assertNotNull(description);
            assertNotEquals("", description);
            switch (option.getNames().getFirst()) {
                case "-notimestamp":
                    assertTrue(option.process("-notimestamp", null));
                    break;
                case "-quiet":
                    assertTrue(option.process("-quiet", null));
                    break;
                case "-windowtitle":
                    assertTrue(option.process("-windowtitle", null));
                    break;
                default:
                    break;
            }
        }
    }

    class TestDocService implements DocService {
        @Override public boolean replacesDefault() { return true; }
        @Override public boolean start(Api api, Context ctx) { return true; }
        @Override public boolean finish() { return true; }
    }

    class TestDocService2 implements DocService {
        @Override public boolean replacesDefault() { return true; }
        @Override public boolean start(Api api, Context ctx) { return true; }
        @Override public boolean finish() { return true; }
    }
}
