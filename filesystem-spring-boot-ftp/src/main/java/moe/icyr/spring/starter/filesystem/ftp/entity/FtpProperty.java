package moe.icyr.spring.starter.filesystem.ftp.entity;

import moe.icyr.spring.starter.filesystem.api.entity.FileSystemProperty;

import java.util.StringJoiner;

/**
 * 个性化配置类
 * @author IceLitty
 * @since 1.0
 */
public class FtpProperty extends FileSystemProperty {

    private String charset;
    private Integer retries;

    public FtpProperty(FileSystemProperty property) {
        super(property);
        this.charset = property.getExternal() == null ? null : String.valueOf(property.getExternal().get("charset"));
        try {
            this.retries = property.getExternal() == null ? null : Integer.valueOf(String.valueOf(property.getExternal().get("retries")));
        } catch (NumberFormatException e) {
            this.retries = null;
        }
    }

    public String getCharset() {
        return charset;
    }

    public FtpProperty setCharset(String charset) {
        this.charset = charset;
        return this;
    }

    public Integer getRetries() {
        return retries;
    }

    public FtpProperty setRetries(Integer retries) {
        this.retries = retries;
        return this;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FtpProperty.class.getSimpleName() + "[", "]")
                .add(super.toString())
                .add("charset='" + charset + "'")
                .add("retries=" + retries)
                .toString();
    }

}
