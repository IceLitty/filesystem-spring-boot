package moe.icyr.spring.starter.filesystem.entity;

import moe.icyr.spring.starter.filesystem.api.entity.FileSystemProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 配置文件类
 *
 * @author IceLitty
 * @since 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "file-system")
public class FileSystemProfile {

    private Map<String, String> factory;
    private FileSystemProperty[] profiles;

    public Map<String, String> getFactory() {
        return factory;
    }

    public FileSystemProfile setFactory(Map<String, String> factory) {
        this.factory = factory;
        return this;
    }

    public FileSystemProperty[] getProfiles() {
        return profiles;
    }

    public FileSystemProfile setProfiles(FileSystemProperty[] profiles) {
        this.profiles = profiles;
        return this;
    }

    /**
     * 根据下标获取配置
     * @param index 下标
     * @return 配置，下标不正确则为NULL
     */
    public FileSystemProperty getProfile(int index) {
        if (this.profiles == null || this.profiles.length == 0)
            return null;
        if (index >= this.profiles.length)
            return null;
        return this.profiles[index];
    }

    /**
     * 根据别名获取配置
     * @param alias 别名
     * @return 配置，别名匹配不到则为NULL，空别名请用下标方式获取
     */
    public FileSystemProperty getProfile(String alias) {
        if (this.profiles == null || this.profiles.length == 0)
            return null;
        if (alias == null || alias.trim().length() == 0)
            return null;
        for (FileSystemProperty prop : this.profiles) {
            if (alias.equals(prop.getAlias())) {
                return prop;
            }
        }
        return null;
    }

    /**
     * 根据别名正则表达式获取配置
     * @param aliasRegex 别名的正则表达式
     * @return 配置，正则匹配不到则为空List
     */
    public List<FileSystemProperty> getProfiles(String aliasRegex) {
        if (this.profiles == null || this.profiles.length == 0)
            return new ArrayList<>();
        Pattern pattern = Pattern.compile(aliasRegex);
        List<FileSystemProperty> filtered = new ArrayList<>();
        for (FileSystemProperty prop : this.profiles) {
            if (prop.getAlias() != null && pattern.matcher(prop.getAlias()).matches()) {
                filtered.add(prop);
            }
        }
        return filtered;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FileSystemProfile.class.getSimpleName() + "[", "]")
                .add("factory=" + factory)
                .add("profiles=" + Arrays.toString(profiles))
                .toString();
    }

}
