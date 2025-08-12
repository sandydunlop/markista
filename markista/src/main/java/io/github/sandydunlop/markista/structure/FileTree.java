package io.github.sandydunlop.markista.structure;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileTree extends AbstractTree  {
    Path rootPath;
    boolean showHidden = false;
    List<String> ignore = new ArrayList<>();
    String separator = java.nio.file.FileSystems.getDefault().getSeparator();

    public void setShowHidden(boolean b) {
        showHidden = b;
    }

    public void ignore(String... files) {
        ignore = Arrays.asList(files);
    }

    public void scan(Path directory) {
        y = 0;
        rootPath = directory;
        root = new TreeNode(NodeKind.FOLDER, directory.getFileName().toString());
        contents = new ArrayList<>();
        contents.add(root);
        y += lineHeight;
        scanDirectory(directory.toFile());
        for (TreeNode treeNode : contents) {
            if (treeNode.getLabel().length() > imageWidth) {
                imageWidth = treeNode.getLabel().length() ;
            }
        }
        imageWidth = (imageWidth + 2) * 12;
        imageHeight = y;
    }

    private void scanDirectory(File directory) {
        File[] filesAndFolders = directory.listFiles();
        if (filesAndFolders != null) {
            List<File> files = scanDirectories(filesAndFolders);
            for (File item : files) {
                Path path = rootPath.relativize(item.toPath());
                TreeNode branch = root;
                if (path.getParent() != null) {
                    String[] pathComponents = path.getParent().toString().split(separator);
                    List<String> list = new ArrayList<>(Arrays.asList(pathComponents));
                    branch = getBranch(root, list);
                }
                String name = path.getFileName().toString();
                TreeNode leaf = new TreeNode(kindFromFilename(name), name);
                leaf.setX(branch.getX() + indent);
                leaf.setY(y);
                leaf.setParent(branch);
                branch.addChild(leaf);
                contents.add(leaf);
                y += lineHeight;
            }
        }
    }

    private List<File> scanDirectories(File[] filesAndFolders) {
        List<File> files = new ArrayList<>();
        for (File item : filesAndFolders) {
            Path path = rootPath.relativize(item.toPath());
            if (!path.getFileName().toString().startsWith(".") || showHidden) {
                if (item.isDirectory()) {
                    if (!ignore.contains(path.getFileName().toString())) {
                        String[] pathComponents = path.toString().split(separator);
                        List<String> list = new ArrayList<>(Arrays.asList(pathComponents));
                        getBranch(root, list);
                        scanDirectory(item);
                    }
                } else {
                    files.add(item);
                }
            }
        }
        return files;
    }

    private NodeKind kindFromFilename(String name) {
        if (name.endsWith(".md")) return NodeKind.DOC;
        if (name.endsWith(".java")) return NodeKind.CODE;
        if (name.endsWith(".svg")) return NodeKind.IMAGE;
        return NodeKind.FILE;
    }
}
