package io.github.sandydunlop.markista.structure;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.util.Context;

class TreeSvgWriterTests {
    private Path tempDir;

    @AfterEach
    void cleanup() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            // Recursively delete files and directories under tempDir
            Files.walk(tempDir)
                .map(Path::toFile)
                .sorted((a, b) -> b.getPath().length() - a.getPath().length()) // delete children first
                .forEach(File::delete);
        }
    }

    // Minimal concrete AbstractTree for testing where we can set root/contents manually
    static class SimpleTree extends AbstractTree {
        public SimpleTree() {
            // nothing
        }
        // Expose a convenience to set contents & root
        public void setRootAndContents(TreeNode r, List<TreeNode> c) {
            this.root = r;
            this.contents = c;
        }
    }

    @Test
    void write_createsSvgFile_withExpectedContents_and_escapesNames() throws Exception {
        tempDir = Files.createTempDirectory("treesvg-test");
        Context ctx = Context.getInstance();
        ctx.setOutputDirectory(tempDir.toString());

        // Prepare a ModuleNode mock so tree.getModuleName() returns a non-empty name
        ModuleNode module = mock(ModuleNode.class);
        when(module.getName()).thenReturn("mymodule");

        // Build a simple tree: root -> child
        TreeNode root = new TreeNode(NodeKind.MODULE, "mymodule");
        root.setX(0);
        root.setY(0);

        TreeNode child = new TreeNode(NodeKind.CODE, "child");
        child.setX(16);
        child.setY(18);
        child.setParent(root);
        root.addChild(child);

        // Also add a node with a dot in the label to check escapeName
        TreeNode dotted = new TreeNode(NodeKind.FILE, "a.b");
        dotted.setX(16);
        dotted.setY(36);
        dotted.setParent(root);
        root.addChild(dotted);

        List<TreeNode> contents = new ArrayList<>();
        contents.add(root);
        contents.add(child);
        contents.add(dotted);

        SimpleTree tree = new SimpleTree();
        tree.setModule(module);
        tree.setRootAndContents(root, contents);
        // Set image dimensions to non-zero so top() includes a height
        tree.imageHeight = 100;

        TreeSvgWriter writer = new TreeSvgWriter();
        // Ensure writer uses the same Context singleton
        writer.setContext(ctx);

        // Write the SVG file
        writer.write(tree, "test.svg");

        // Verify file exists in output directory under module name
        Path outFile = tempDir.resolve(module.getName()).resolve("test.svg");
        assertTrue(Files.exists(outFile), "SVG file should be created at " + outFile);

        // Read file contents
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(outFile.toFile()))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        String content = sb.toString();

        // Basic checks: header, svg tags, defs
        assertTrue(content.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"), "SVG header should be present");
        assertTrue(content.contains("<svg"), "SVG root element should be present");
        assertTrue(content.contains("<defs/>") || content.contains("<defs />"), "defs should be present");

        // Check that module and child nodes were emitted with data-cell-id attributes.
        // For root label "mymodule" writer.escapeName produces "mymodule" => data-cell-id "module_mymodule"
        assertTrue(content.contains("data-cell-id=\"module_mymodule\""), "data-cell-id for module should be present");
        // Child has name "child" -> data-cell-id "module_child"
        assertTrue(content.contains("data-cell-id=\"module_child\""), "data-cell-id for child should be present");
        // Icon group for child should exist
        assertTrue(content.contains("data-cell-id=\"module_child_icon\""), "icon group for child should be present");

        // Check escapeName replaced dot with underscore for "a.b" -> module_a_b
        assertTrue(content.contains("data-cell-id=\"module_a_b\""), "escaped name for dotted label should be present");

        // Check that label text elements were written (text element contains label)
        assertTrue(content.contains(">child</text>") || content.contains(">child</text>") , "label text for child should be present");

        // Because child had a parent, the writer should have written connecting path(s)
        // Check for quadratic path "Q" or path M ... L segments as emitted by writeLines1
        assertTrue(content.contains("path") && content.contains("stroke:"), "path elements with stroke style should be present for connecting lines");

        // Ensure the writer closed the root groups properly (end svg)
        assertTrue(content.trim().endsWith("</svg>"), "output should end with closing svg tag");
    }

}
