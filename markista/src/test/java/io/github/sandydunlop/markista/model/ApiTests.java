package io.github.sandydunlop.markista.model;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiTests {
    public static Stream<Arguments> commonBaseCases() {
        return Stream.of(
            // two different top-level packages -> no common base
            Arguments.of(List.of("one", "zero"), ""),
            Arguments.of(List.of("zero", "one"), ""),

            // one and one.two -> no common base (first package is top-level)
            Arguments.of(List.of("one", "one.two"), ""),

            // siblings under 'one' -> common base is 'one'
            Arguments.of(List.of("one.ten", "one.two"), "one"),

            // deeper/nested combinations
            Arguments.of(List.of("one.two.three", "one.two"), "one"),
            Arguments.of(List.of("one.two", "one.two.three"), "one"),

            // single package -> trim last segment
            Arguments.of(List.of("one.two.three"), "one.two"),

            // sibling deep packages -> common base 'one.two'
            Arguments.of(List.of("one.two.three", "one.two.zero"), "one.two"),
            Arguments.of(List.of("one.two.zero", "one.two.three"), "one.two"),

            // parent/child deep relationship -> common base 'one.two'
            Arguments.of(List.of("one.two.three", "one.two.three.four"), "one.two"),
            Arguments.of(List.of("one.two.three.four", "one.two.three"), "one.two"),

            // two vs four-depth -> common base 'one'
            Arguments.of(List.of("one.two", "one.two.three.four"), "one"),
            Arguments.of(List.of("one.two.three.four", "one.two"), "one")
        );
    }

    @ParameterizedTest(name = "commonBase for packages {0} => ''{1}''")
    @MethodSource("commonBaseCases")
    void commonBase_various(List<String> packageNames, String expectedBase) {
        Api api = new Api("Test API");
        for (String pkg : packageNames) {
            api.addPackage(new PackageNode(pkg));
        }
        String base = api.commonBase();
        assertEquals(expectedBase, base);
    }
}
