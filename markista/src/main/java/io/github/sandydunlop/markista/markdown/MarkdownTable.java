package io.github.sandydunlop.markista.markdown;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/// A utility class for creating tables in Markdown documents.
/// 
/// This class allows adding columns with headings and rows with data.
/// It automatically calculates column widths to align the table content.
/// 
/// The table can be rendered as Markdown text to a Writer, optionally with indentation for nested formatting.
public class MarkdownTable {
    List<Column> columns = new ArrayList<>();
    List<String[]> rows = new ArrayList<>();
    
    /// Constructs an empty MarkdownTable.
    public MarkdownTable() {
        // Nothing to do here
    }

    /// Adds a column with the specified heading to the table.
    /// @param heading String to be used as the column heading.
    /// @return The table (this) to allow method chaining.
    public MarkdownTable addColumn(String heading) {
        columns.add(new Column(heading));
        return this;
    }

    /// Adds a row of data to the table.
    /// The number of values should match or be less than the number of columns.
    /// @param valueStrings One or more strings representing the data for this row.
    /// @return The table (this) to allow method chaining.
    public MarkdownTable addRow(String...valueStrings) {
        rows.add(valueStrings);
        // Update column widths if any value is longer than current width
        for (int i = 0; i < valueStrings.length; i++) {
            Column column = columns.get(i);
            if (valueStrings[i] != null && valueStrings[i].length() > column.width) {
                column.width = valueStrings[i].length();
            }
        }
        return this;
    }

    /// Renders the table as Markdown text without any indentation.
    /// @param writer The Writer to output Markdown text to.
    /// @throws IOException If an error occurs during writing.
    public void render(Writer writer) throws IOException {
        render(writer, 0);
    }

    /// Renders the table as Markdown text with a given indentation level (number of spaces).
    /// @param writer The Writer to output Markdown text to.
    /// @param indent The number of spaces to indent each line.
    /// @throws IOException If an error occurs during writing.
    public void render(Writer writer, int indent) throws IOException {
        writer.write(" ".repeat(indent));
        // Write header line with column headings
        for (Column column : columns) {
            writer.write("| ");
            writer.write(column.name);
            // Padding spaces to align to column width + 1 trailing space
            writer.write(" ".repeat(column.width - column.name.length() + 1));
        }
        writer.write("|\n");

        writer.write(" ".repeat(indent));
        // Write separator line with dashes for Markdown table header separator
        for (Column column : columns) {
            writer.write("|");
            writer.write("-".repeat(column.width + 2));
        }
        writer.write("|\n");

        // Write data rows
        for (String[] data : rows) {
            writer.write(" ".repeat(indent));
            for (int i = 0; i < columns.size(); i++) {
                writer.write("| ");
                writer.write(data[i]);
                // Padding spaces for alignment + 1 trailing space
                writer.write(" ".repeat(columns.get(i).width - data[i].length() + 1));
            }
            writer.write("|\n");
        }
        writer.write("\n\n");
        writer.flush();
    }

    /// A class to represent the name and width of a column within a table.
    public class Column {
        String name;
        int width;

        /// Constructs a new `Column` object with the specified name.
        /// @param name The name (heading) of the column.
        public Column(String name) {
            this.name = name;
            this.width = name.length();
        }
    }
}