package moe.icyr.spring.starter.filesystem.api.entity;

import java.util.Collection;
import java.util.StringJoiner;

/**
 * 文件信息对象
 *
 * @author IceLitty
 * @since 1.0
 */
public class FileInfo<T> {

    private String absolutePath;
    private String filename;
    private Long size;
    private boolean isFile;
    private boolean isDirectory;
    private Collection<FileInfo<T>> children;
    private T originalInfo;

    public String getAbsolutePath() {
        return absolutePath;
    }

    public FileInfo<T> setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
        return this;
    }

    public String getFilename() {
        return filename;
    }

    public FileInfo<T> setFilename(String filename) {
        this.filename = filename;
        return this;
    }

    public Long getSize() {
        return size;
    }

    public FileInfo<T> setSize(Long size) {
        this.size = size;
        return this;
    }

    public boolean isFile() {
        return isFile;
    }

    public FileInfo<T> setFile(boolean file) {
        isFile = file;
        return this;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public FileInfo<T> setDirectory(boolean directory) {
        isDirectory = directory;
        return this;
    }

    public Collection<FileInfo<T>> getChildren() {
        return children;
    }

    public FileInfo<T> setChildren(Collection<FileInfo<T>> children) {
        this.children = children;
        return this;
    }

    public T getOriginalInfo() {
        return originalInfo;
    }

    public void setOriginalInfo(T originalInfo) {
        this.originalInfo = originalInfo;
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
