package io.github.sandydunlop.markista.model;

import java.io.Serial;
import java.io.Serializable;

public class SourceCodeLocation  implements Serializable{
    @Serial
    private static final long serialVersionUID = 1L;

    private String fileName = "";
    private int lineNumber = 0;
    private int endLineNumber = 0;

    private SourceCodeLocation() {
        // Nothing to see here
    }

    public SourceCodeLocation(String fileName, int lineNumber, int endLineNumber) {
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.endLineNumber = endLineNumber;
    }

    public static SourceCodeLocation undefined() {
        return new SourceCodeLocation();
    }

    public void setFileName(String s) {
        fileName = s;
    }

    public String getFileName() {
        return fileName;
    }

    public void setLineNumber(int n) {
        lineNumber = n;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getEndLineNumber() {
        return endLineNumber;
    }

    public boolean isEmpty() {
        return fileName.isEmpty();
    }
}
