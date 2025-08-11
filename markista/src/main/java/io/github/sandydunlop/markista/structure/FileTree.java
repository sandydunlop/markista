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

    public void setShowHidden(boolean b) {
        showHidden = b;
    }

    public void ignore(String... files) {
        for (String file : files) {
            ignore.add(file);
        }
    }

    private void scanDirectory(File directory) {
        String separator = java.nio.file.FileSystems.getDefault().getSeparator();
        File[] filesAndFolders = directory.listFiles();
        if (filesAndFolders != null) {
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

    private NodeKind kindFromFilename(String name) {
        if (name.endsWith(".md")) return NodeKind.DOC;
        if (name.endsWith(".java")) return NodeKind.CODE;
        if (name.endsWith(".svg")) return NodeKind.IMAGE;
        return NodeKind.FILE;
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

    // TODO: MOve this to abstract
    private TreeNode getBranch(TreeNode fromBranch, List<String> components) {
        String branchName = components.getFirst();
        TreeNode branch = fromBranch.getChild(branchName);
        if (branch == null) {
            branch = new TreeNode(NodeKind.FOLDER, branchName);
            fromBranch.addChild(branch);
            branch.setX(fromBranch.getX() + indent);
            branch.setY(y);
            branch.setParent(fromBranch);
            contents.add(branch);
            y += lineHeight;
        }
        if (components.size() > 1) {
            components.removeFirst();
            return getBranch(branch, components);
        }
        return branch;
    } 
}
