package io.github.sandydunlop.markista.structure;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.sandydunlop.markista.model.ModuleNode;

class AbstractTreeTests {

    // Concrete minimal subclass so we can instantiate AbstractTree
    static class TestTree extends AbstractTree {
        // expose protected members or behavior for testing if needed
        public TreeNode invokeGetBranch(TreeNode from, List<String> components) {
            // We won't call super.getBranch in tests because the AbstractTree code expects
            // List with getFirst/removeFirst (LinkedList). We avoid that here.
            // This helper exists if you later want to forward a LinkedList safely:
            return null;
        }
    }

    private TestTree tree;

    @BeforeEach
    void setUp() {
        tree = new TestTree();
    }

    @Test
    void getModuleName_whenNoModule_returnsUnnamed() {
        // module is null by default
        assertEquals("unnamed module", tree.getModuleName());
    }

    @Test
    void getModuleName_returnsModuleNameWhenSet() {
        ModuleNode mockedModule = mock(ModuleNode.class);
        when(mockedModule.getName()).thenReturn("com.example.module");

        tree.setModule(mockedModule);

        assertEquals("com.example.module", tree.getModuleName());
    }

    @Test
    void makeAllVisible_setsAllNodesVisible() {
        // prepare contents with nodes some visible, some not
        TreeNode n1 = new TreeNode(NodeKind.FILE, "one");
        TreeNode n2 = new TreeNode(NodeKind.FILE, "two");
        TreeNode n3 = new TreeNode(NodeKind.FILE, "three");

        n1.setVisible(false);
        n2.setVisible(true);
        n3.setVisible(false);

        tree.contents = new ArrayList<>();
        tree.contents.add(n1);
        tree.contents.add(n2);
        tree.contents.add(n3);

        tree.makeAllVisible();

        for (TreeNode n : tree.getContents()) {
            assertTrue(n.isVisible(), "All nodes should be visible after makeAllVisible()");
        }
    }

    @Test
    void hideNames_hidesMatchingNodeLabelsOnly() {
        TreeNode keep = new TreeNode(NodeKind.FILE, "keep-me");
        TreeNode hideA = new TreeNode(NodeKind.FILE, "secret-file-A");
        TreeNode hideB = new TreeNode(NodeKind.FILE, "very-secret-B");

        keep.setVisible(true);
        hideA.setVisible(true);
        hideB.setVisible(true);

        tree.contents = new ArrayList<>();
        tree.contents.add(keep);
        tree.contents.add(hideA);
        tree.contents.add(hideB);

        tree.hideNames("secret");

        assertTrue(keep.isVisible(), "Non-matching label should remain visible");
        assertFalse(hideA.isVisible(), "Label containing 'secret' should be hidden");
        assertFalse(hideB.isVisible(), "Label containing 'secret' should be hidden");
    }

    @Test
    void hideKind_hidesNodesOfGivenKindOnly() {
        TreeNode code1 = new TreeNode(NodeKind.CODE, "CodeOne");
        TreeNode doc1 = new TreeNode(NodeKind.DOC, "DocOne");
        TreeNode code2 = new TreeNode(NodeKind.CODE, "CodeTwo");

        code1.setVisible(true);
        doc1.setVisible(true);
        code2.setVisible(true);

        tree.contents = new ArrayList<>();
        tree.contents.add(code1);
        tree.contents.add(doc1);
        tree.contents.add(code2);

        tree.hideKind(NodeKind.CODE);

        assertFalse(code1.isVisible(), "CODE nodes should be hidden");
        assertTrue(doc1.isVisible(), "Non-CODE nodes should remain visible");
        assertFalse(code2.isVisible(), "CODE nodes should be hidden");
    }

    @Test
    void getters_and_setters_forRootContentsAndDimensions() {
        TreeNode root = new TreeNode(NodeKind.MODULE, "root");
        List<TreeNode> contents = new ArrayList<>();
        contents.add(root);

        tree.root = root;
        tree.contents = contents;
        tree.imageWidth = 123;
        tree.imageHeight = 456;

        assertSame(root, tree.getRoot());
        assertSame(contents, tree.getContents());
        assertEquals(123, tree.getWidth());
        assertEquals(456, tree.getHeight());
    }

    @Test
    void setModule_affectsGetModuleName() {
        ModuleNode mocked = mock(ModuleNode.class);
        when(mocked.getName()).thenReturn("modX");

        tree.setModule(mocked);
        assertEquals("modX", tree.getModuleName());
    }
}