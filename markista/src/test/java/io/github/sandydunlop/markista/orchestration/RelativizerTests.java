package io.github.sandydunlop.markista.orchestration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RelativizerTests {
	@Disabled
	@Test
	void relativize_fromNoLevel() {
		// String path = Relativizer.relativize("", "io.github.sandydunlop.markista.model.Node");
		// assertEquals("io/github/sandydunlop/markista/model/Node", path);
	}

	@Disabled
	@Test
	void relativize_squashed_fromNoLevel() {
		// Relativizer.setFlattenedDirectories("io.github.sandydunlop.markista");
		// String path = Relativizer.relativize("", "io.github.sandydunlop.markista.doclet");
		// assertEquals("doclet", path);
	}

	@Disabled
	@Test
	void relativize_squashed_toSameLevel() {
		// Relativizer.setFlattenedDirectories("io.github.sandydunlop.markista");
		// String path = Relativizer.relativize("io.github.sandydunlop.markista.util", "io.github.sandydunlop.markista.doclet");
		// assertEquals("../doclet", path);
	}

	@Disabled
	@Test
	void relativize_toParentLevel() {
		// String path = Relativizer.relativize("io.github.sandydunlop.markista.util", "io.github.sandydunlop.markista");
		// assertEquals("..", path);
	}

	@Disabled
	@Test
	void relativize_toSameLevel() {
		// String path = Relativizer.relativize("io.github.sandydunlop.markista.util", "io.github.sandydunlop.markista.doclet");
		// assertEquals("../doclet", path);
	}
}
