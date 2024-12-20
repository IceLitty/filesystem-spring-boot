package moe.icyr.spring.starter.filesystem.api.entity;

import java.util.Map;
import java.util.StringJoiner;

/**
 * 配置文件实体类
 *
 * @author IceLitty
 * @since 1.0
 */
public class FileSystemProperty {

    private String type;
    private String alias;
    private String ip;
    private Integer port;
    private String username;
    private String password;
    private Map<String, Object> external;

    public FileSystemProperty() {
    }

    @SuppressWarnings("CopyConstructorMissesField")
    public FileSystemProperty(FileSystemProperty property) {
        this.type = property.getType();
        this.alias = property.getAlias();
        this.ip = property.getIp();
        this.port = property.getPort();
        this.username = property.getUsername();
        this.password = property.getPassword();
    }

    public String getType() {
        return type;
    }

    public FileSystemProperty setType(String type) {
        this.type = type;
        return this;
    }

    public String getAlias() {
        return alias;
    }

    public FileSystemProperty setAlias(String alias) {
        this.alias = alias;
        return this;
    }

    public String getIp() {
        return ip;
    }

    public FileSystemProperty setIp(String ip) {
        this.ip = ip;
        return this;
    }

    public Integer getPort() {
        return port;
    }

    public FileSystemProperty setPort(Integer port) {
        this.port = port;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public FileSystemProperty setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public FileSystemProperty setPassword(String password) {
        this.password = password;
        return this;
    }

    public Map<String, Object> getExternal() {
        return external;
    }

    public FileSystemProperty setExternal(Map<String, Object> external) {
        this.external = external;
        return this;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FileSystemProperty.class.getSimpleName() + "[", "]")
                .add("type='" + type + "'")
                .add("alias='" + alias + "'")
                .add("ip='" + ip + "'")
                .add("port=" + port)
                .add("username='" + username + "'")
                .add("password='" + (password == null ? "null" : "***") + "'")
                .toString();
    }

}
