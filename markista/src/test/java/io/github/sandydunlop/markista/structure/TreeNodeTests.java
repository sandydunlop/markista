package io.github.sandydunlop.markista.structure;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

class TreeNodeTests {

    private TreeNode root;

    @BeforeEach
    void setUp() {
        root = new TreeNode(NodeKind.MODULE, "root");
    }

    @Test
    void constructor_initializesFields() {
        assertEquals(NodeKind.MODULE, root.getKind(), "Kind should be set from constructor");
        assertEquals("root", root.getLabel(), "Label should be set from constructor");
        assertNotNull(root.getIcon(), "Icon should be created by constructor");
        assertEquals(0, root.getX(), "Default x should be 0");
        assertEquals(0, root.getY(), "Default y should be 0");
        assertTrue(root.isVisible(), "Default visibility should be true");
        assertNull(root.getParent(), "Parent should be null by default");
        assertNull(root.getPath(), "Path should be null by default");
        List<TreeNode> children = root.children; // package-private access is available in same package
        assertNotNull(children, "Children list should be initialized");
        assertTrue(children.isEmpty(), "Children list should be empty initially");
    }

    @Test
    void addChild_and_getChild_returnsChildByLabel() {
        TreeNode child = new TreeNode(NodeKind.PACKAGE, "child");
        // addChild does not set parent, so set it to mimic usage elsewhere
        root.addChild(child);
        child.setParent(root);

        TreeNode found = root.getChild("child");
        assertNotNull(found, "getChild should return the child with matching label");
        assertSame(child, found, "Returned child should be the same instance that was added");
        assertSame(root, child.getParent(), "Parent should have been set");
    }

    @Test
    void getChild_returnsFirstMatchingChild_whenMultipleWithSameLabel() {
        TreeNode first = new TreeNode(NodeKind.PACKAGE, "dup");
        TreeNode second = new TreeNode(NodeKind.PACKAGE, "dup");

        root.addChild(first);
        root.addChild(second);

        TreeNode found = root.getChild("dup");
        assertNotNull(found, "getChild should find a child with the given label");
        assertSame(first, found, "getChild should return the first matching child added");
    }

    @Test
    void getChild_returnsNull_whenNotFound() {
        TreeNode child = new TreeNode(NodeKind.PACKAGE, "exists");
        root.addChild(child);

        TreeNode notFound = root.getChild("missing");
        assertNull(notFound, "getChild should return null when no child matches the label");
    }

    @Test
    void position_and_visibility_setters_and_getters() {
        root.setX(42);
        root.setY(84);
        assertEquals(42, root.getX(), "X coordinate should reflect setX");
        assertEquals(84, root.getY(), "Y coordinate should reflect setY");

        root.setVisible(false);
        assertFalse(root.isVisible(), "Visibility should be false after setVisible(false)");
        root.setVisible(true);
        assertTrue(root.isVisible(), "Visibility should be true after setVisible(true)");
    }

    @Test
    void path_and_parent_setters_and_getters() {
        Path p = Path.of("some", "path", "File.java");
        root.setPath(p);
        assertEquals(p, root.getPath(), "getPath should return the Path previously set");

        TreeNode parent = new TreeNode(NodeKind.MODULE, "parent");
        root.setParent(parent);
        assertSame(parent, root.getParent(), "getParent should return the parent previously set");
    }

    @Test
    void kind_setter_updatesKind_but_icon_remains_initial() {
        TreeNode node = new TreeNode(NodeKind.PACKAGE, "pkg");
        assertEquals(NodeKind.PACKAGE, node.getKind(), "Initial kind should be PACKAGE");
        assertNotNull(node.getIcon());
        assertEquals(NodeKind.PACKAGE, node.getIcon().getKind(), "Icon kind initially matches constructor kind");

        // Change kind
        node.setKind(NodeKind.CODE);
        assertEquals(NodeKind.CODE, node.getKind(), "getKind should reflect the new kind after setKind");
        // Icon was created in constructor and is not updated by setKind()
        assertEquals(NodeKind.PACKAGE, node.getIcon().getKind(), "Icon kind remains the original kind created at construction");
    }
}