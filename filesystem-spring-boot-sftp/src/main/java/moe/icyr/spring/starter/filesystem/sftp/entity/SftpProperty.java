package moe.icyr.spring.starter.filesystem.sftp.entity;

import moe.icyr.spring.starter.filesystem.api.entity.FileSystemProperty;

import java.util.StringJoiner;

/**
 * 个性化配置类
 * @author IceLitty
 * @since 1.0
 */
public class SftpProperty extends FileSystemProperty {

    private String privateKey;
    private Integer keepAliveSecond;

    public SftpProperty(FileSystemProperty property) {
        super(property);
        this.privateKey = null;
        this.keepAliveSecond = null;
        if (property.getExternal() != null) {
            if (property.getExternal().get("privateKey") == null) {
                if (property.getExternal().get("private-key") != null) {
                    this.privateKey = String.valueOf(property.getExternal().get("private-key"));
                }
            } else {
                this.privateKey = String.valueOf(property.getExternal().get("privateKey"));
            }
            try {
                if (property.getExternal().get("keepAliveSecond") == null) {
                    if (property.getExternal().get("keep-alive-second") != null) {
                        this.keepAliveSecond = Integer.valueOf(String.valueOf(property.getExternal().get("keep-alive-second")));
                    }
                } else {
                    this.keepAliveSecond = Integer.valueOf(String.valueOf(property.getExternal().get("keepAliveSecond")));
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public SftpProperty setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
        return this;
    }

    public Integer getKeepAliveSecond() {
        return keepAliveSecond;
    }

    public SftpProperty setKeepAliveSecond(Integer keepAliveSecond) {
        this.keepAliveSecond = keepAliveSecond;
        return this;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SftpProperty.class.getSimpleName() + "[", "]")
                .add(super.toString())
                .add("privateKey=" + (privateKey == null ? null : "***"))
                .add("keepAliveSecond=" + keepAliveSecond)
                .toString();
    }

}
