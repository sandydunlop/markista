package io.github.sandydunlop.markista.doclet;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/// A utility class for creating tables in Markdown documents.
public class MarkdownTable {
    List<Column> columns = new ArrayList<>();
    List<String[]> rows = new ArrayList<>();
    
    public MarkdownTable() {
        // Nothing to do here
    }

    /// Adds a column with the specified heading to the table
    /// @param heading String to be used as the column heading
    /// @return The table
    public MarkdownTable addColumn(String heading) {
        columns.add(new Column(heading));
        return this;
    }

    /// Adds a row of data to the table
    /// @param valueStrings  one or more strings which represnet the data for this row
    /// @return              The table
    public MarkdownTable addRow(String...valueStrings) {
        rows.add(valueStrings);
        for (int i=0; i<valueStrings.length; i++) {
            Column column = columns.get(i);
            if (valueStrings[i] != null && valueStrings[i].length() > column.width) {
                column.width = valueStrings[i].length();
            }
        }
        return this;
    }

    /// Renders the table as Markdown text without any indentation
    public void render(Writer writer) throws IOException {
        render(writer, 0);
    }

    /// Renders the table as Markdown text with a given indentation level
    public void render(Writer writer, int indent) throws IOException {
        writer.write(" ".repeat(indent));
        for (Column column : columns) {
            writer.write("| ");
            writer.write(column.name);
            writer.write(" ".repeat(column.width - column.name.length() + 1));
        }
        writer.write("|\n");
        writer.write(" ".repeat(indent));
        for (Column column : columns) {
            writer.write("|");
            writer.write("-".repeat(column.width + 2));
        }
        writer.write("|\n");
        for (String[] data : rows) {
            writer.write(" ".repeat(indent));
            for (int i=0; i<columns.size(); i++) {
                writer.write("| ");
                writer.write(data[i]);
                writer.write(" ".repeat(columns.get(i).width - data[i].length() + 1));
            }
            writer.write("|\n");
        }
        writer.flush();
    }

    /// A class to represent the name and width of a column within a table
    public class Column {
        String name;
        int width = 0;
        /// Constructs a new `Column` object with the specified name.
        /// @param name The name of the new `Column`
        public Column(String name) {
            this.name = name;
            this.width = name.length();
        }
    }
}

