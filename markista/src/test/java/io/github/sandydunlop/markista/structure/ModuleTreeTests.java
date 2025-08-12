package io.github.sandydunlop.markista.structure;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.TypeNode;

class ModuleTreeTests {

    @BeforeEach
    void setUp() {
        // Nothing global to init for ModuleTree tests
    }

    @Test
    void scan_withEmptyModule_createsSingleModuleNode() {
        // Mock a ModuleNode with no packages and no module-info
        ModuleNode module = mock(ModuleNode.class);
        when(module.getName()).thenReturn("com.example.module");
        when(module.getPackages()).thenReturn(new ArrayList<>()); // no packages
        when(module.hasModuleInfo()).thenReturn(false);

        ModuleTree tree = new ModuleTree();
        tree.setModule(module);

        tree.scan();

        List<TreeNode> contents = tree.getContents();
        assertNotNull(contents, "contents should not be null");

        // Only one node (the module)
        assertEquals(1, contents.size(), "Should contain only the module node");

        TreeNode root = tree.getRoot();
        assertNotNull(root, "root should be set");
        assertEquals(NodeKind.MODULE, root.getKind(), "root kind should be MODULE");
        assertEquals("com.example.module", root.getLabel(), "root label should be module name");
        // width computed as (maxLabelLength + 2) * 12
        int expectedWidth = (root.getLabel().length() + 2) * 12;
        assertEquals(expectedWidth, tree.getWidth(), "image width should match computed value");

        // height is lineHeight * number of nodes; default lineHeight is 18 and y increments once per node
        int expectedHeight = contents.size() * 18;
        assertEquals(expectedHeight, tree.getHeight(), "image height should be correct");
    }

    @Test
    void scan_withPackageAndType_createsModulePackageAndTypeNodes() {
        // Mock a TypeNode
        TypeNode type = mock(TypeNode.class);
        when(type.getSimpleName()).thenReturn("Thing");

        // Mock a PackageNode that contains the above type and no subpackages, and no package-info
        PackageNode pkg = mock(PackageNode.class);
        when(pkg.getName()).thenReturn("com.example");
        List<PackageNode> emptyPkgList = new ArrayList<>();
        when(pkg.getPackages()).thenAnswer( _ -> emptyPkgList);
        when(pkg.hasPackageInfo()).thenReturn(false);
        List<TypeNode> types = new ArrayList<>();
        types.add(type);
        when(pkg.getTypes()).thenReturn(types);

        // Mock a ModuleNode that contains the package and no module-info
        ModuleNode module = mock(ModuleNode.class);
        when(module.getName()).thenReturn("modA");
        List<PackageNode> pkgs = new ArrayList<>();
        // The code in ModuleTree iterates PackageMember and checks instanceof PackageNode;
        // providing a mock created as PackageNode.class makes the instanceof check succeed.
        pkgs.add(pkg);
        when(module.getPackages()).thenReturn((List) pkgs);
        when(module.hasModuleInfo()).thenReturn(false);

        ModuleTree tree = new ModuleTree();
        tree.setModule(module);

        tree.scan();

        List<TreeNode> contents = tree.getContents();
        assertNotNull(contents, "contents should not be null");

        // Expect module + package + type => 3 nodes
        assertEquals(3, contents.size(), "Should contain module, package and type nodes");

        // Check ordering and kinds:
        TreeNode moduleNode = contents.get(0);
        TreeNode packageNode = contents.get(1);
        TreeNode typeNode = contents.get(2);

        assertEquals(NodeKind.MODULE, moduleNode.getKind(), "first node should be MODULE");
        assertEquals("modA", moduleNode.getLabel(), "module label should match");

        assertEquals(NodeKind.PACKAGE, packageNode.getKind(), "second node should be PACKAGE");
        assertEquals("com.example", packageNode.getLabel(), "package label should match");

        assertEquals(NodeKind.CODE, typeNode.getKind(), "third node should be CODE (type)");
        assertEquals("Thing", typeNode.getLabel(), "type label should be the simple name returned by TypeNode.getSimpleName()");

        // image width should be computed from the longest label among the nodes
        int maxLabelLen = 0;
        for (TreeNode n : contents) {
            if (n.getLabel().length() > maxLabelLen) maxLabelLen = n.getLabel().length();
        }
        int expectedWidth = (maxLabelLen + 2) * 12;
        assertEquals(expectedWidth, tree.getWidth(), "computed image width mismatch");

        // height should equal 18 * 3
        int expectedHeight = contents.size() * 18;
        assertEquals(expectedHeight, tree.getHeight(), "computed image height mismatch");
    }

    @Test
    void scan_unnamedModule_becomesUnnamedModuleLabel() {
        ModuleNode module = mock(ModuleNode.class);
        when(module.getName()).thenReturn(""); // blank name triggers "unnamed module"
        when(module.getPackages()).thenReturn(new ArrayList<>());
        when(module.hasModuleInfo()).thenReturn(false);

        ModuleTree tree = new ModuleTree();
        tree.setModule(module);
        tree.scan();

        List<TreeNode> contents = tree.getContents();
        assertNotNull(contents);
        assertEquals(1, contents.size());
        TreeNode n = contents.get(0);
        assertEquals("unnamed module", n.getLabel());
        assertEquals(NodeKind.MODULE, n.getKind());
    }
}