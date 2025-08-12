package io.github.sandydunlop.markista.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IconTests {
    @Test
    void factoryMethods_returnIconsWithExpectedKinds() {
        Icon m = Icon.module();
        Icon p = Icon.pkg();
        Icon c = Icon.code();
        Icon f = Icon.folder();
        Icon d = Icon.doc();
        Icon docIcon = Icon.doc();
        Icon imageIcon = new Icon(NodeKind.IMAGE); // direct construction for coverage

        assertNotNull(m);
        assertNotNull(p);
        assertNotNull(c);
        assertNotNull(f);
        assertNotNull(d);
        assertNotNull(docIcon);
        assertNotNull(imageIcon);

        assertEquals(NodeKind.MODULE, m.getKind());
        assertEquals(NodeKind.PACKAGE, p.getKind());
        assertEquals(NodeKind.CODE, c.getKind());
        assertEquals(NodeKind.FOLDER, f.getKind());
        assertEquals(NodeKind.DOC, d.getKind());
        assertEquals(NodeKind.DOC, docIcon.getKind());
        assertEquals(NodeKind.IMAGE, imageIcon.getKind());
    }

    @Test
    void placeAt_module_rendersSvg_withCoordinates_and_modulePath() {
        Icon icon = new Icon(NodeKind.MODULE);
        String svg = icon.placeAt(10, 20);

        assertNotNull(svg);
        assertTrue(svg.contains("<svg"), "SVG root element should be present");
        assertTrue(svg.contains("x=\"10\""), "x coordinate should appear in the svg element");
        assertTrue(svg.contains("y=\"20\""), "y coordinate should appear in the svg element");
        // module icon contains a distinctive path string
        assertTrue(svg.contains("M21 14L12 20L3 14"), "module path should be present");
        assertTrue(svg.trim().endsWith("</svg>"), "svg output should be closed");
    }

    @Test
    void placeAt_package_rendersSvg_withCoordinates_and_packagePath() {
        Icon icon = new Icon(NodeKind.PACKAGE);
        String svg = icon.placeAt(5, 6);

        assertNotNull(svg);
        assertTrue(svg.contains("<svg"), "SVG root element should be present");
        assertTrue(svg.contains("x=\"5\""));
        assertTrue(svg.contains("y=\"6\""));
        // package icon contains distinctive numeric coordinates in path 'M20.929,1.628'
        assertTrue(svg.contains("M20.929,1.628") || svg.contains("M20.929, 1.628"),
                "package path signature should be present");
        assertTrue(svg.endsWith("\n"), "output ends with newline(s) and closing svg");
    }

    @Test
    void placeAt_code_rendersSvg_withCoordinates_and_vectorId() {
        Icon icon = new Icon(NodeKind.CODE);
        String svg = icon.placeAt(1, 2);

        assertNotNull(svg);
        assertTrue(svg.contains("<svg"));
        assertTrue(svg.contains("x=\"1\""));
        assertTrue(svg.contains("y=\"2\""));
        // code icon includes "M13 3.00087" and id="Vector" in one branch
        assertTrue(svg.contains("M13 3.00087") || svg.contains("M13.0004 3.00087") || svg.contains("id=\"Vector\""),
                "code path or id should be present");
    }

    @Test
    void placeAt_folder_rendersSvg_withCoordinates_and_folderPath() {
        Icon icon = new Icon(NodeKind.FOLDER);
        String svg = icon.placeAt(7, 8);

        assertNotNull(svg);
        assertTrue(svg.contains("<svg"));
        assertTrue(svg.contains("x=\"7\""));
        assertTrue(svg.contains("y=\"8\""));
        // folder path has "M3 8.2C3 7.07989" sequence in the implementation
        assertTrue(svg.contains("M3 8.2") || svg.contains("M3 8.2C3 7.07989"),
                "folder path signature should be present");
    }

    @Test
    void placeAt_doc_rendersSvg_withCoordinates_and_docPath() {
        Icon icon = new Icon(NodeKind.DOC);
        String svg = icon.placeAt(11, 12);

        assertNotNull(svg);
        assertTrue(svg.contains("<svg"));
        assertTrue(svg.contains("x=\"11\""));
        assertTrue(svg.contains("y=\"12\""));
        // doc icon has "M9 17H15" line and other doc-specific path pieces
        assertTrue(svg.contains("M9 17H15") || svg.contains("M13.0004 3.00087"),
                "doc path signature should be present");
    }

    @Test
    void placeAt_image_rendersSvg_withCoordinates_and_imagePath() {
        Icon icon = new Icon(NodeKind.IMAGE);
        String svg = icon.placeAt(3, 4);

        assertNotNull(svg);
        assertTrue(svg.contains("<svg"));
        assertTrue(svg.contains("x=\"3\""));
        assertTrue(svg.contains("y=\"4\""));
        // image icon contains long signature starting with M3.00005
        assertTrue(svg.contains("M3.00005 17.0001") || svg.contains("L7.76798"),
                "image path signature should be present");
    }

    @Test
    void placeAt_file_usesDefaultBranch_and_containsCoordinates() {
        // NodeKind.FILE falls through to the default 'else' branch in Icon.placeAt
        Icon icon = new Icon(NodeKind.FILE);
        String svg = icon.placeAt(99, 100);

        assertNotNull(svg);
        assertTrue(svg.contains("<svg"));
        assertTrue(svg.contains("x=\"99\""));
        assertTrue(svg.contains("y=\"100\""));
        // Default branch uses a similar path to CODE; check for that signature
        assertTrue(svg.contains("M13 3.00087") || svg.contains("M13.0004 3.00087") || svg.contains("stroke-width=\"2\""),
                "default/file path signature should be present");
    }
}
