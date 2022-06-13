package moe.icyr.spring.starter.filesystem.api.entity;

import java.util.Collection;
import java.util.StringJoiner;

/**
 * 文件信息对象
 *
 * @author IceLitty
 * @since 1.0
 */
public class FileInfo {

    private String absolutePath;
    private String filename;
    private Long size;
    private boolean isFile;
    private boolean isDirectory;
    private Collection<FileInfo> children;

    public String getAbsolutePath() {
        return absolutePath;
    }

    public FileInfo setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
        return this;
    }

    public String getFilename() {
        return filename;
    }

    public FileInfo setFilename(String filename) {
        this.filename = filename;
        return this;
    }

    public Long getSize() {
        return size;
    }

    public FileInfo setSize(Long size) {
        this.size = size;
        return this;
    }

    public boolean isFile() {
        return isFile;
    }

    public FileInfo setFile(boolean file) {
        isFile = file;
        return this;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public FileInfo setDirectory(boolean directory) {
        isDirectory = directory;
        return this;
    }

    public Collection<FileInfo> getChildren() {
        return children;
    }

    public FileInfo setChildren(Collection<FileInfo> children) {
        this.children = children;
        return this;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FileInfo.class.getSimpleName() + "[", "]")
                .add("absolutePath='" + absolutePath + "'")
                .add("filename='" + filename + "'")
                .add("size='" + size + "'")
                .add("isFile=" + isFile)
                .add("isDirectory=" + isDirectory)
                .add("children=" + children)
                .toString();
    }

}
