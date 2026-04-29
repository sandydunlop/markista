package io.github.sandydunlop.markista.orchestration;

public class Relativizer {
    // private static String flattenedDirectories = null;

    private Relativizer() {
        // Nothing to see here
    }

    // /// Sets the string used to adjust flattened directories in relative path calculations.
    // /// @param fd The string representing flattened directories.
    // public static void setFlattenedDirectories(String fd) {
    //     flattenedDirectories = fd;
    // }

    /// Produces a relative path string from one package to another by splitting and comparing components.
    /// Supports flattened directories if set.
    /// @param from The source package name.
    /// @param to The target package name.
    /// @return The relative path string.
    public static String relativize(String from, String to, String commonBasePath) {
        if (from == null || to == null) return "";
        from = flattenDirectory(from, commonBasePath);
        to = flattenDirectory(to, commonBasePath);
        String[] fromParts = from.split("\\.");
        String[] toParts = to.split("\\.");
        int commonIndex = findCommonIndex(fromParts, toParts);
        StringBuilder rel = new StringBuilder();
        if (!from.isEmpty()) {
            appendParentDirs(rel, fromParts.length - commonIndex);
        }
        appendTargetDirs(rel, toParts, commonIndex);
        return rel.toString();
    }

    /// Removes prefix directories from a path if flattenedDirectories is set and matches.
    /// @param path The package name or path to flatten.
    /// @return The adjusted path or original if no flattening applies.
    static String flattenDirectory(String path, String commonBasePath) {
        if (commonBasePath != null && !commonBasePath.isEmpty() && path.startsWith(commonBasePath)) {
            if (path.length() <= commonBasePath.length()) {
                return "";
            }
            return path.substring(commonBasePath.length() + 1);
        }
        return path;
    }

    /// Finds the common prefix index between two string arrays.
    /// @param fromParts Array of strings for source path.
    /// @param toParts Array of strings for target path.
    /// @return The number of common leading segments.
    static int findCommonIndex(String[] fromParts, String[] toParts) {
        if (fromParts.length == 0 || toParts.length == 0) {
            return 0;
        }
        int len = Math.min(fromParts.length, toParts.length);
        int i = 0;
        while (i < len && fromParts[i].equals(toParts[i])) {
            i++;
        }
        return i;
    }

    /// Appends parent directory segments `..` to the relative path string builder.
    /// @param rel The StringBuilder accumulating the path.
    /// @param count The number of parent directory segments to append.
    static void appendParentDirs(StringBuilder rel, int count) {
        for (int i = 0; i < count; i++) {
            if (!rel.isEmpty()) rel.append("/");
            rel.append("..");
        }
    }

    /// Appends target directory segments to the relative path string builder starting at index start.
    /// @param rel The StringBuilder accumulating the path.
    /// @param toParts Array of target path segments.
    /// @param start The start index for appending segments.
    static void appendTargetDirs(StringBuilder rel, String[] toParts, int start) {
        for (int i = start; i < toParts.length; i++) {
            if (toParts[i].isEmpty()) continue;
            if (!rel.isEmpty()) rel.append("/");
            rel.append(toParts[i]);
        }
    }
}
