package moe.icyr.spring.starter.filesystem.api;

import moe.icyr.spring.starter.filesystem.api.entity.FileInfo;
import moe.icyr.spring.starter.filesystem.api.entity.FileSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Base64;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

/**
 * 文件服务器连接器
 *
 * @author IceLitty
 * @since 1.0
 */
public abstract class FileSystem implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(FileSystem.class);
    private static final Pattern base64UrlPrefixPattern = Pattern.compile("^data:\\w+/?\\w*;base64,");
    private static final String base64UrlPrefixReplaceString = "data:\\w+/?\\w*;base64,";
    private static final ResourceBundle message = ResourceBundle.getBundle("Message");

    /**
     * 该实例的配置类
     */
    protected final FileSystemProperty property;

    /**
     * 验证配置并创建连接
     * @param property 配置文件
     */
    public FileSystem(FileSystemProperty property) {
        this.property = validateProperty(property);
        this.init(this.property);
        this.connect();
    }

    /**
     * 校验配置
     * @param property 配置
     * @return 存放在 {@code this.property} 的对象
     */
    public FileSystemProperty validateProperty(FileSystemProperty property) {
        if (property == null) {
            throw new IllegalArgumentException(message.getString("fs.property.valid.not.null"));
        }
        if (property.getType() == null) {
            throw new IllegalArgumentException(message.getString("fs.property.type.valid.fail"));
        }
        if (property.getIp() == null || property.getIp().trim().length() == 0) {
            throw new IllegalArgumentException(message.getString("fs.property.ip.valid.fail"));
        }
        if (property.getPort() == null || property.getPort() < 0 || property.getPort() > 65535) {
            throw new IllegalArgumentException(message.getString("fs.property.port.valid.fail"));
        }
        if (property.getUsername() == null || property.getUsername().trim().length() == 0) {
            throw new IllegalArgumentException(message.getString("fs.property.username.valid.fail"));
        }
        if (property.getPassword() == null) {
            property.setPassword("");
        }
        return property;
    }

    /**
     * 初始化操作
     * @param property {@code validateProperty(FileSystemProperty)} 方法返回的配置类对象
     */
    protected abstract void init(FileSystemProperty property);

    /**
     * 内部连接方法
     * @return 连接成功与否
     */
    protected abstract boolean connect();

    /**
     * 断开连接
     */
    public abstract void disconnect();

    /**
     * 列出文件(夹)
     * @param path 绝对路径
     * @return 文件(夹)信息列表，若没有这个目录、FTP连接失败、或其他异常则返回NULL
     */
    public Collection<FileInfo> list(String path) {
        return list(path, false, false);
    }

    /**
     * 列出文件(夹)
     * @param path 绝对路径
     * @param deepFind 是否读取子文件
     * @param flatPrint 是否扁平化输出
     * @return 文件(夹)信息列表，若没有这个目录、FTP连接失败、或其他异常则返回NULL
     */
    public abstract Collection<FileInfo> list(String path, boolean deepFind, boolean flatPrint);

    /**
     * 查询文件信息
     * @param path 绝对路径
     * @param filename 文件名
     * @return 文件信息对象
     */
    public FileInfo peekFile(String path, String filename) {
        return null;
    }

    /**
     * <p>上传文件，通过输入流</p>
     * <p>提供给特殊需求使用的方法</p>
     * @param input 需要上传的流
     * @param path 目标文件绝对路径
     * @param filename 目标文件名称
     * @return 上传成功与否
     */
    public boolean upload(InputStream input, StringBuffer path, StringBuffer filename) {
        return upload(input, path.toString(), filename.toString());
    }

    /**
     * <p>上传文件，通过字节</p>
     * <p>提供给特殊需求使用的方法</p>
     * @param bytes 需要上传的字节
     * @param path 目标文件绝对路径
     * @param filename 目标文件名称
     * @return 上传成功与否
     */
    public boolean upload(byte[] bytes, StringBuffer path, StringBuffer filename) {
        return _upload(bytes, path, filename);
    }

    /**
     * <p>上传文件，通过Base64</p>
     * <p>提供给特殊需求使用的方法</p>
     * @param base64 需要上传的Base64
     * @param path 目标文件绝对路径
     * @param filename 目标文件名称
     * @return 上传成功与否
     */
    public boolean upload(String base64, StringBuffer path, StringBuffer filename) {
        return _upload(base64, path, filename);
    }

    /**
     * 上传文件，通过输入流
     * @param input 需要上传的流
     * @param path 目标文件绝对路径
     * @param filename 目标文件名称
     * @return 上传成功与否
     */
    public abstract boolean upload(InputStream input, String path, String filename);

    /**
     * 上传文件，通过字节
     * @param bytes 需要上传的字节
     * @param path 目标文件绝对路径
     * @param filename 目标文件名称
     * @return 上传成功与否
     */
    public boolean upload(byte[] bytes, String path, String filename) {
        return _upload(bytes, path, filename);
    }

    /**
     * 上传文件，通过Base64
     * @param base64 需要上传的Base64
     * @param path 目标文件绝对路径
     * @param filename 目标文件名称
     * @return 上传成功与否
     */
    public boolean upload(String base64, String path, String filename) {
        return _upload(base64, path, filename);
    }

    private boolean _upload(byte[] bytes, Object path, Object filename) {
        try (ByteArrayInputStream stream = new ByteArrayInputStream(bytes)) {
            if (path instanceof StringBuffer && filename instanceof StringBuffer) {
                return upload(stream, (StringBuffer) path, (StringBuffer) filename);
            } else if (path instanceof String && filename instanceof String) {
                return upload(stream, (String) path, (String) filename);
            } else {
                return false;
            }
        } catch (Exception e) {
            log.error(message.getString("fs.upload.fail.io")
                    .replace("${path}", String.valueOf(path))
                    .replace("${filename}", String.valueOf(filename))
                    .replace("${length}", bytes == null ? "-1" : Integer.toString(bytes.length)), e);
        }
        return false;
    }

    private boolean _upload(String base64, Object path, Object filename) {
        byte[] bytes = _base64ToBytes(base64, path, filename);
        if (bytes == null) {
            return false;
        }
        if (path instanceof StringBuffer && filename instanceof StringBuffer) {
            return upload(bytes, (StringBuffer) path, (StringBuffer) filename);
        } else if (path instanceof String && filename instanceof String) {
            return upload(bytes, (String) path, (String) filename);
        } else {
            return false;
        }
    }

    private byte[] _base64ToBytes(String base64, Object path, Object filename) {
        if (base64UrlPrefixPattern.matcher(base64).find()) {
            base64 = base64.replaceFirst(base64UrlPrefixReplaceString, "");
        }
        try {
            return Base64.getDecoder().decode(base64);
        } catch (Exception e) {
            log.error(message.getString("fs.upload.fail.base64")
                    .replace("${path}", String.valueOf(path))
                    .replace("${filename}", String.valueOf(filename)), e);
            return null;
        }
    }

    /**
     * 根据给定路径创建至目录
     * @param path 绝对路径
     * @return 创建成功与否
     */
    public abstract boolean createDirectory(String path);

    /**
     * 下载为Base64
     * @param path 绝对路径
     * @param filename 文件名
     * @return Base64
     */
    public String downloadBase64(String path, String filename) {
        byte[] bytes = downloadBytes(path, filename);
        if (bytes == null) {
            return null;
        }
        return new String(Base64.getEncoder().encode(bytes));
    }

    /**
     * 下载为字节数组
     * @param path 绝对路径
     * @param filename 文件名
     * @return 字节数组
     */
    public byte[] downloadBytes(String path, String filename) {
        ByteArrayOutputStream stream = downloadStream(path, filename);
        if (stream == null) {
            return null;
        }
        return stream.toByteArray();
    }

    /**
     * 下载为字节流
     * @param path 绝对路径
     * @param filename 文件名
     * @return 字节流
     */
    public abstract ByteArrayOutputStream downloadStream(String path, String filename);

    /**
     * 删除文件
     * @param path 绝对路径
     * @param filename 文件名
     * @return 成功与否
     */
    public abstract boolean deleteFile(String path, String filename);

    /**
     * 关闭连接
     */
    @Override
    public void close() {
        this.disconnect();
    }

    /**
     * 为可能的断点续传提供接口支持
     * @param base64 BASE64
     * @param path 绝对路径
     * @param filename 文件名
     * @param fileSize 当前块大小
     * @param fileOffset 从文件该偏移量处继续上传
     * @return 成功与否
     */
    public boolean appenderUpload(String base64, StringBuffer path, StringBuffer filename, long fileSize, long fileOffset) {
        byte[] bytes = _base64ToBytes(base64, path.toString(), filename.toString());
        if (bytes == null) {
            return false;
        }
        return appenderUpload(bytes, path, filename, fileSize, fileOffset);
    }

    /**
     * 为可能的断点续传提供接口支持
     * @param bytes 要上传的字节
     * @param path 绝对路径
     * @param filename 文件名
     * @param fileSize 当前块大小
     * @param fileOffset 从文件该偏移量处继续上传
     * @return 成功与否
     */
    public boolean appenderUpload(byte[] bytes, StringBuffer path, StringBuffer filename, long fileSize, long fileOffset) {
        try (ByteArrayInputStream stream = new ByteArrayInputStream(bytes)) {
            return appenderUpload(stream, path, filename, fileSize, fileOffset);
        } catch (Exception e) {
            log.error(message.getString("fs.upload.fail.io")
                    .replace("${path}", String.valueOf(path))
                    .replace("${filename}", String.valueOf(filename))
                    .replace("${length}", bytes == null ? "-1" : Integer.toString(bytes.length)), e);
        }
        return false;
    }

    /**
     * 为可能的断点续传提供接口支持
     * @param input 输入流
     * @param path 绝对路径
     * @param filename 文件名
     * @param fileSize 当前块大小
     * @param fileOffset 从文件该偏移量处继续上传
     * @return 成功与否
     */
    public boolean appenderUpload(InputStream input, StringBuffer path, StringBuffer filename, long fileSize, long fileOffset) {
        return false;
    }

}
