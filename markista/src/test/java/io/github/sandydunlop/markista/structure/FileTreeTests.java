package io.github.sandydunlop.markista.structure;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class FileTreeTests {

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

    @Test
    void scan_createsTreeForFilesInRoot_andComputesDimensions() throws IOException {
        tempDir = Files.createTempDirectory("filetree-test-root");
        // Create files of different kinds
        Path md = Files.createFile(tempDir.resolve("readme.md"));
        Path java = Files.createFile(tempDir.resolve("Thing.java"));
        Path svg = Files.createFile(tempDir.resolve("image.svg"));
        Path other = Files.createFile(tempDir.resolve("notes.txt"));

        FileTree ft = new FileTree();
        ft.setShowHidden(false);
        ft.setModule(null); // not required but explicit
        ft.scan(tempDir);

        // root + 4 files
        List<TreeNode> contents = ft.getContents();
        assertNotNull(contents, "contents should not be null");
        // Expect 1 root + 4 leaves
        assertEquals(5, contents.size(), "should contain root plus four file entries");

        TreeNode root = ft.getRoot();
        assertNotNull(root, "root should be present");
        assertEquals(tempDir.getFileName().toString(), root.getLabel(), "root label should be directory name");

        // Check kinds by name
        boolean foundMd = false, foundJava = false, foundSvg = false, foundOther = false;
        int maxLabelLength = 0;
        for (TreeNode n : contents) {
            String label = n.getLabel();
            if (label.equals("readme.md")) {
                assertEquals(NodeKind.DOC, n.getKind(), "readme.md should be DOC");
                foundMd = true;
            } else if (label.equals("Thing.java")) {
                assertEquals(NodeKind.CODE, n.getKind(), "Thing.java should be CODE");
                foundJava = true;
            } else if (label.equals("image.svg")) {
                assertEquals(NodeKind.IMAGE, n.getKind(), "image.svg should be IMAGE");
                foundSvg = true;
            } else if (label.equals("notes.txt")) {
                assertEquals(NodeKind.FILE, n.getKind(), "notes.txt should be FILE");
                foundOther = true;
            }
            if (label.length() > maxLabelLength) maxLabelLength = label.length();
        }
        assertTrue(foundMd && foundJava && foundSvg && foundOther, "All created files must be discovered");

        // verify image width and height calculations:
        // imageWidth is set to (maxLabelLength + 2) * 12
        int expectedWidth = (maxLabelLength + 2) * 12;
        assertEquals(expectedWidth, ft.getWidth(), "computed image width mismatch");

        // imageHeight is y at end: lineHeight * number_of_entries
        // AbstractTree default lineHeight is 18, initial root occupies one line then one per file
        int expectedHeight = 18 * contents.size();
        assertEquals(expectedHeight, ft.getHeight(), "computed image height mismatch");
    }

    @Test
    void scan_respectsShowHiddenFlag() throws IOException {
        tempDir = Files.createTempDirectory("filetree-test-hidden");
        // create hidden and visible files
        Path visible = Files.createFile(tempDir.resolve("visible.md"));
        Path hidden = Files.createFile(tempDir.resolve(".secret"));

        FileTree ft = new FileTree();
        ft.setShowHidden(false);
        ft.scan(tempDir);

        // When showHidden is false, .secret should be excluded
        boolean hasHidden = ft.getContents().stream().anyMatch(n -> n.getLabel().equals(".secret"));
        assertFalse(hasHidden, "hidden file should be excluded when showHidden is false");

        // Now enable showHidden and rescan
        ft = new FileTree();
        ft.setShowHidden(true);
        ft.scan(tempDir);

        hasHidden = ft.getContents().stream().anyMatch(n -> n.getLabel().equals(".secret"));
        assertTrue(hasHidden, "hidden file should be included when showHidden is true");
    }

    @Test
    void ignore_directory_isSkipped() throws IOException {
        tempDir = Files.createTempDirectory("filetree-test-ignore");
        // Create a file in root and a directory named "build" with a file inside
        Path rootFile = Files.createFile(tempDir.resolve("keep.md"));
        Path buildDir = Files.createDirectory(tempDir.resolve("build"));
        Path ignoredFile = Files.createFile(buildDir.resolve("ignored.txt"));

        FileTree ft = new FileTree();
        ft.setShowHidden(true);
        ft.ignore("build"); // ensure build directory is ignored
        ft.scan(tempDir);

        // The ignored file should not be present
        boolean foundIgnored = ft.getContents().stream().anyMatch(n -> n.getLabel().equals("ignored.txt"));
        assertFalse(foundIgnored, "Files inside ignored directories should not be present after scan");

        // The root file should be present
        boolean foundRoot = ft.getContents().stream().anyMatch(n -> n.getLabel().equals("keep.md"));
        assertTrue(foundRoot, "Root file should be present");
    }
}