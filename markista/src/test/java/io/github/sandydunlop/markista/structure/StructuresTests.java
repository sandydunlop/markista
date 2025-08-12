package io.github.sandydunlop.markista.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.util.List;
import java.lang.reflect.Method;

import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.util.Configuration;
import io.github.sandydunlop.markista.util.Context;

class StructuresTests {

    Context ctx = Context.getInstance();

    @AfterEach
    void tearDown() throws Exception {
        // Reset output directory and project path (if setter exists) between tests
        ctx.setOutputDirectory("");
        // Try to clear project path if a setter exists (use reflection to avoid compile errors if method absent)
        try {
            Method setProjectPath = Configuration.class.getMethod("setProjectPath", String.class);
            setProjectPath.invoke(null, new Object[] { (String) null });
        } catch (NoSuchMethodException ignored) {
            // If the method doesn't exist in the runtime Configuration, nothing to reset
        }
    }

    @Test
    void run_withoutProjectPath_invokesWriterForDocsAndModule() throws Exception {
        // Prepare environment
        Context ctx = Context.getInstance();
        // Set a non-empty output directory so the Path passed to scan is predictable
        ctx.setOutputDirectory("outdir");

        // Create a mock ModuleNode
        ModuleNode module = mock(ModuleNode.class);
        when(module.getName()).thenReturn("mymod");

        // Use Mockito to mock constructions of TreeSvgWriter, FileTree and ModuleTree so no real IO occurs
        try (MockedConstruction<TreeSvgWriter> mockedWriter = Mockito.mockConstruction(TreeSvgWriter.class);
            MockedConstruction<FileTree> mockedFileTree = Mockito.mockConstruction(FileTree.class);
            MockedConstruction<ModuleTree> mockedModuleTree = Mockito.mockConstruction(ModuleTree.class)) {

            Structures s = new Structures();
            // ensure Structures uses the same Context instance
            s.setContext(ctx);
            s.setModule(module);

            s.run();

            // Exactly one TreeSvgWriter should have been constructed
            List<TreeSvgWriter> writers = mockedWriter.constructed();
            assertEquals(1, writers.size(), "One TreeSvgWriter should be constructed");
            TreeSvgWriter writerMock = writers.get(0);
            // verify writer had its context set
            verify(writerMock).setContext(eq(ctx));

            // Exactly one FileTree should have been constructed (docTree)
            List<FileTree> fileTrees = mockedFileTree.constructed();
            assertEquals(1, fileTrees.size(), "One FileTree should be constructed when projectPath is null");
            FileTree docTreeMock = fileTrees.get(0);

            // verify docTree was configured with the module and scanned at the expected path
            verify(docTreeMock).setModule(eq(module));
            Path expectedDocScanPath = Path.of(ctx.getOutputDirectory(), module.getName());
            verify(docTreeMock).scan(eq(expectedDocScanPath));

            // Exactly one ModuleTree should have been constructed
            List<ModuleTree> modules = mockedModuleTree.constructed();
            assertEquals(1, modules.size(), "One ModuleTree should be constructed");
            ModuleTree moduleTreeMock = modules.get(0);

            // verify module tree configured and scanned
            verify(moduleTreeMock).setModule(eq(module));
            verify(moduleTreeMock).scan();

            // verify writer.write was invoked for docs.svg and module.svg
            verify(writerMock).write(eq(docTreeMock), eq("docs.svg"));
            verify(writerMock).write(eq(moduleTreeMock), eq("module.svg"));

            // And ensure no attempt was made to write a source.svg (projectPath was null)
            verify(writerMock, never()).write(any(FileTree.class), eq("source.svg"));
        }
    }

    @Test
    void run_withProjectPath_invokesWriterForDocsSourceAndModule_and_ignoresBuildAndBin() throws Exception {
        // Prepare environment
        Context ctx = Context.getInstance();
        ctx.setOutputDirectory("outdir");

        // Set a project path via reflection if the setter exists; otherwise attempt to set a field is not done.
        String projectPath = "projectRoot";
        boolean projectPathSet = false;
        try {
            Method setProjectPath = Configuration.class.getMethod("setProjectPath", String.class);
            setProjectPath.invoke(null, projectPath);
            projectPathSet = true;
        } catch (NoSuchMethodException ignored) {
            // If Configuration does not expose project path setters in this runtime, skip this assertion path
        }

        // Create a mock ModuleNode
        ModuleNode module = mock(ModuleNode.class);
        when(module.getName()).thenReturn("mymod");

        try (MockedConstruction<TreeSvgWriter> mockedWriter = Mockito.mockConstruction(TreeSvgWriter.class);
            MockedConstruction<FileTree> mockedFileTree = Mockito.mockConstruction(FileTree.class);
            MockedConstruction<ModuleTree> mockedModuleTree = Mockito.mockConstruction(ModuleTree.class)) {

            Structures s = new Structures();
            s.setContext(ctx);
            s.setModule(module);

            s.run();

            // Verify TreeSvgWriter used
            TreeSvgWriter writerMock = mockedWriter.constructed().get(0);
            verify(writerMock).setContext(eq(ctx));

            // When projectPath is set, two FileTree objects are constructed: docTree and sourceTree
            List<FileTree> fileTrees = mockedFileTree.constructed();
            assertTrue(fileTrees.size() >= 1, "At least one FileTree should be constructed");
            // The first constructed FileTree is the docTree
            FileTree docTreeMock = fileTrees.get(0);
            verify(docTreeMock).setModule(eq(module));
            verify(docTreeMock).scan(eq(Path.of(ctx.getOutputDirectory(), module.getName())));
            // If projectPath setter exists we expect a second FileTree (the source tree)
            if (projectPathSet) {
                assertTrue(fileTrees.size() >= 2, "Source FileTree should be constructed when projectPath is set");
                FileTree sourceTreeMock = fileTrees.get(1);
                // verify ignore("build") and ignore("bin") were called (two separate calls)
                verify(sourceTreeMock).setModule(eq(module));
                // The run method calls ignore("build"); ignore("bin");
                verify(sourceTreeMock).ignore(eq("build"));
                verify(sourceTreeMock).ignore(eq("bin"));
                // verify it scanned the project path
                verify(sourceTreeMock).scan(eq(Path.of(projectPath)));
                // verify writer.write called for source.svg
                verify(writerMock).write(eq(sourceTreeMock), eq("source.svg"));
            }

            // Verify module tree behavior unchanged
            ModuleTree moduleTreeMock = mockedModuleTree.constructed().get(0);
            verify(moduleTreeMock).setModule(eq(module));
            verify(moduleTreeMock).scan();

            // verify writer.write was invoked for docs.svg and module.svg
            verify(writerMock).write(eq(docTreeMock), eq("docs.svg"));
            verify(writerMock).write(eq(moduleTreeMock), eq("module.svg"));
        } finally {
            // Try to clear project path so other tests are not affected
            try {
                Method setProjectPath = Configuration.class.getMethod("setProjectPath", String.class);
                setProjectPath.invoke(null, new Object[] { (String) null });
            } catch (NoSuchMethodException ignored) {
            }
        }
    }
}
