package io.github.sandydunlop.markista.orchestration;

import io.github.sandydunlop.markista.core.Context;

import java.io.File;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;

import com.sun.source.util.DocTreePath;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import jdk.javadoc.doclet.Reporter;

class ModulePathTests {
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

    @Test
    void processDirectoryUrl_callsProcessClassFileForClassFiles() throws Exception {
        Path tempDir = Files.createTempDirectory("lr-processdir");
        File dir = tempDir.toFile();
        try {
            // create a dummy class file in the directory
            File classFile = new File(dir, "MyClass.class");
            assertTrue(classFile.createNewFile(), "create dummy class file");

            URL[] urls = new URL[] { dir.toURI().toURL() };

            ModulePath modulePath = spy(ModulePath.class);
            modulePath.ctx = Context.getInstance();
            modulePath.ctx.setReporter(reporter);

            when(modulePath.processClassFile(any(File.class), any(File.class), any(URLClassLoader.class), any())).thenAnswer(invocation -> {
                File directoryArg = invocation.getArgument(0);
                File fileArg = invocation.getArgument(1);
                // assert the arguments are as expected inside the stub
                assertEquals(dir.getAbsoluteFile(), directoryArg.getAbsoluteFile());
                assertEquals(classFile.getName(), fileArg.getName());
                return null;
            });

            modulePath.processDirectoryUrl(dir, urls, null);
            verify(modulePath).processClassFile(any(), any(), any(), any());
        } finally {
            // cleanup
            Files.deleteIfExists(tempDir.resolve("MyClass.class"));
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    void processClassFile_readsModuleInfoAndSetsSiblingModuleName() throws Exception {
        Path tempDir = Files.createTempDirectory("lr-moduleinfo");
        File dir = tempDir.toFile();
        File moduleInfo = new File(dir, "module-info.class");
        ModulePath modulePath = new ModulePath(Context.getInstance(), "");
        try {
            assertTrue(moduleInfo.createNewFile(), "create module-info.class placeholder");

            // Mock ModuleDescriptor.read to avoid parsing actual class bytes
            ModuleDescriptor fakeDescriptor = mock(ModuleDescriptor.class);
            when(fakeDescriptor.name()).thenReturn("fake.module");

            try (MockedStatic<ModuleDescriptor> mdStatic = Mockito.mockStatic(ModuleDescriptor.class)) {
                mdStatic.when(() -> ModuleDescriptor.read(any(InputStream.class))).thenReturn(fakeDescriptor);

                // classLoader not needed for module-info branch; pass null
                String r = modulePath.processClassFile(dir, moduleInfo, null, null);

                assertEquals("fake.module", r);
            }
        } finally {
            Files.deleteIfExists(moduleInfo.toPath());
            Files.deleteIfExists(tempDir);
        }
    }
}
