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
        this.privateKey = property.getExternal() == null ? null :
                (property.getExternal().get("privateKey") != null ?
                        String.valueOf(property.getExternal().get("privateKey")) :
                        String.valueOf(property.getExternal().get("private-key")));
        try {
            this.keepAliveSecond = property.getExternal() == null ? null :
                    (property.getExternal().get("keepAliveSecond") != null ?
                            Integer.valueOf(String.valueOf(property.getExternal().get("keepAliveSecond"))) :
                            Integer.valueOf(String.valueOf(property.getExternal().get("keep-alive-second"))));
        } catch (NumberFormatException e) {
            this.keepAliveSecond = null;
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
