package moe.icyr.spring.starter.filesystem;

import moe.icyr.spring.starter.filesystem.entity.FileSystemProfile;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

/**
 * 自动配置类
 *
 * @author IceLitty
 * @since 1.0
 */
@Configuration
@EnableConfigurationProperties(FileSystemProfile.class)
@ConditionalOnProperty(prefix = "file-system", value = "enabled", matchIfMissing = true)
public class FileSystemAutoConfiguration implements ApplicationContextAware {

    private static ApplicationContext context;

    public FileSystemAutoConfiguration(@Autowired FileSystemProfile profile) {}

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    public static ApplicationContext getContext() {
        return context;
    }

}
