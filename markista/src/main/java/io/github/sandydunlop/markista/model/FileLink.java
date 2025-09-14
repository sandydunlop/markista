package io.github.sandydunlop.markista.model;

public class FileLink extends Link {
    String label = "";
    String fileName = "";

    private FileLink() {
        // Nothing to see here
    }

    /// Sets the target of the link to the provided filename.
    /// @param file The filename to use
    /// @return the link with its target filename set
    public static FileLink to(String file) {
        FileLink link = new FileLink();
        link.fileName = file;
        link.scope = Scope.UNKNOWN;
        link.kind = Kind.FILE;
        return link;
    }

    public FileLink from(Reference ref) {
        this.origin = ref;
        return this;
    }

    public FileLink withLabel(String s) {
        this.label = s;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public String getFileName() {
        return fileName;
    }
}
