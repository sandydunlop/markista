package io.github.sandydunlop.markista.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ApiTests {
    @Test
    void commonBase_one() {
        Api api = new Api("Test API");
        api.addPackage(new PackageNode("one"));
        api.addPackage(new PackageNode("zero"));
        String base = api.commonBase();
        assertEquals("", base);
    }

    @Test
    void commonBase_one_reversed() {
        Api api = new Api("Test API");
        api.addPackage(new PackageNode("zero"));
        api.addPackage(new PackageNode("one"));
        String base = api.commonBase();
        assertEquals("", base);
    }

    @Test
    void commonBase_one_two() {
        Api api = new Api("Test API");
        api.addPackage(new PackageNode("one"));
        api.addPackage(new PackageNode("one.two"));
        String base = api.commonBase();
        assertEquals("", base);
    }

    @Test
    void commonBase_two_two() {
        Api api = new Api("Test API");
        api.addPackage(new PackageNode("one.ten"));
        api.addPackage(new PackageNode("one.two"));
        String base = api.commonBase();
        assertEquals("one", base);
    }

    @Test
    void commonBase_two_three() {
        Api api = new Api("Test API");
        api.addPackage(new PackageNode("one.two.three"));
        api.addPackage(new PackageNode("one.two"));
        String base = api.commonBase();
        assertEquals("one", base);
    }

    @Test
    void commonBase_two_three_reversed() {
        Api api = new Api("Test API");
        api.addPackage(new PackageNode("one.two"));
        api.addPackage(new PackageNode("one.two.three"));
        String base = api.commonBase();
        assertEquals("one", base);
    }

    @Test
    void commonBase_three_singlePackage() {
        Api api = new Api("Test API");
        api.addPackage(new PackageNode("one.two.three"));
        String base = api.commonBase();
        assertEquals("one.two", base);
    }

    @Test
    void commonBase_three() {
        Api api = new Api("Test API");
        api.addPackage(new PackageNode("one.two.three"));
        api.addPackage(new PackageNode("one.two.zero"));
        String base = api.commonBase();
        assertEquals("one.two", base);
    }

    @Test
    void commonBase_three_reversed() {
        Api api = new Api("Test API");
        api.addPackage(new PackageNode("one.two.zero"));
        api.addPackage(new PackageNode("one.two.three"));
        String base = api.commonBase();
        assertEquals("one.two", base);
    }

    @Test
    void commonBase_three_four() {
        Api api = new Api("Test API");
        api.addPackage(new PackageNode("one.two.three"));
        api.addPackage(new PackageNode("one.two.three.four"));
        String base = api.commonBase();
        assertEquals("one.two", base);
    }

    @Test
    void commonBase_three_four_reversed() {
        Api api = new Api("Test API");
        api.addPackage(new PackageNode("one.two.three.four"));
        api.addPackage(new PackageNode("one.two.three"));
        String base = api.commonBase();
        assertEquals("one.two", base);
    }

    @Test
    void commonBase_two_four() {
        Api api = new Api("Test API");
        api.addPackage(new PackageNode("one.two"));
        api.addPackage(new PackageNode("one.two.three.four"));
        String base = api.commonBase();
        assertEquals("one", base);
    }

    @Test
    void commonBase_two_four_reversed() {
        Api api = new Api("Test API");
        api.addPackage(new PackageNode("one.two.three.four"));
        api.addPackage(new PackageNode("one.two"));
        String base = api.commonBase();
        assertEquals("one", base);
    }
}
