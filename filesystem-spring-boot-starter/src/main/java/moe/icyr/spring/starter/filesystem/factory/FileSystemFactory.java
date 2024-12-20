package moe.icyr.spring.starter.filesystem.factory;

import moe.icyr.spring.starter.filesystem.FileSystemAutoConfiguration;
import moe.icyr.spring.starter.filesystem.api.FileSystem;
import moe.icyr.spring.starter.filesystem.api.entity.FileSystemProperty;
import moe.icyr.spring.starter.filesystem.entity.FileSystemProfile;
import moe.icyr.spring.starter.filesystem.fdfs.FdfsFileSystem;
import moe.icyr.spring.starter.filesystem.ftp.FtpFileSystem;
import moe.icyr.spring.starter.filesystem.minio.MinIOFileSystem;
import moe.icyr.spring.starter.filesystem.sftp.SftpSshjFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ResourceBundle;

/**
 * 工厂类
 *
 * @author IceLitty
 * @since 1.0
 */
public class FileSystemFactory {

    private static final Logger log = LoggerFactory.getLogger(FileSystemFactory.class);
    private static final ResourceBundle message = ResourceBundle.getBundle("MessageStarter");

    @SuppressWarnings("unchecked")
    public static <T, F> FileSystem<T, F> make(FileSystemProperty property) {
        if (property == null || property.getType() == null) {
            throw new IllegalArgumentException(message.getString("fs.starter.not.specified.type"));
        }
        FileSystemProfile profile;
        //noinspection ConstantConditions
        if (FileSystemAutoConfiguration.getContext() == null
                || (profile = FileSystemAutoConfiguration.getContext().getBean(FileSystemProfile.class)) == null) {
            throw new IllegalArgumentException(message.getString("fs.starter.profile.bean.not.autowired"));
        }
        String clazz = profile.getFactory() == null ? null : profile.getFactory().get(property.getType());
        // Use default object
        if (clazz == null) {
            switch (property.getType().toLowerCase()) {
                case "ftp":
                    return (FileSystem<T, F>) new FtpFileSystem(property);
                case "sftp":
                    return (FileSystem<T, F>) new SftpSshjFileSystem(property);
                case "fdfs":
                case "fastdfs":
                    return (FileSystem<T, F>) new FdfsFileSystem(property);
                case "minio":
                    return (FileSystem<T, F>) new MinIOFileSystem(property);
                default:
                    throw new IllegalArgumentException(message.getString("fs.starter.factory.not.found")
                            .replace("${class}", "null"));
            }
        }
        FileSystem<T, F> fs;
        Class<?> client;
        try {
            client = Class.forName(clazz);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(message.getString("fs.starter.factory.not.found")
                    .replace("${class}", clazz));
        }
        Constructor<?> constructor;
        try {
            constructor = client.getConstructor(FileSystemProperty.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(message.getString("fs.starter.factory.constructor.not.found")
                    .replace("${class}", clazz));
        }
        try {
            fs = (FileSystem<T, F>) constructor.newInstance(property);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException(message.getString("fs.starter.factory.build.error"), e);
        }
        return fs;
    }

}
