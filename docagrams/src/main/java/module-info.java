import io.github.sandydunlop.markista.spi.DocService;

module docagrams {
    requires markista;

    provides DocService with io.github.sandydunlop.docagrams.demo.Demo;
}
