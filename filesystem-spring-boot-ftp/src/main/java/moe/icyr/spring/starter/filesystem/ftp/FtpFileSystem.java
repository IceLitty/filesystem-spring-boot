package moe.icyr.spring.starter.filesystem.ftp;

import moe.icyr.spring.starter.filesystem.api.FileSystem;
import moe.icyr.spring.starter.filesystem.api.entity.FileInfo;
import moe.icyr.spring.starter.filesystem.api.entity.FileSystemProperty;
import moe.icyr.spring.starter.filesystem.ftp.entity.FtpProperty;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;

/**
 * FTP文件服务连接器
 *
 * @author IceLitty
 * @since 1.0
 */
public class FtpFileSystem extends FileSystem<FTPClient, FTPFile> {

    private static final Logger log = LoggerFactory.getLogger(FtpFileSystem.class);
    private static final ResourceBundle message = ResourceBundle.getBundle("MessageFtp");

    private FtpProperty property;

    private FTPClient ftpClient;

    /**
     * 剩余重连次数
     */
    protected int retryCountdown;

    /**
     * 验证配置并创建连接
     *
     * @param property 配置文件
     */
    public FtpFileSystem(FileSystemProperty property) {
        super(property);
    }

    @Override
    public FtpProperty validateProperty(FileSystemProperty property) {
        super.validateProperty(property);
        FtpProperty ftpProperty;
        if (property instanceof FtpProperty) {
            ftpProperty = (FtpProperty) property;
        } else {
            ftpProperty = new FtpProperty(property);
        }
        if (ftpProperty.getCharset() == null) {
            ftpProperty.setCharset("UTF-8");
        }
        if (ftpProperty.getRetries() == null || ftpProperty.getRetries() < 0) {
            ftpProperty.setRetries(0);
        }
        return ftpProperty;
    }

    @Override
    protected void init(FileSystemProperty property) {
        this.property = (FtpProperty) property;
        this.retryCountdown = this.property.getRetries();
    }

    @Override
    protected boolean connect() {
        if (this.retryCountdown < 0) {
            log.error(message.getString("fs.ftp.connect.fail.retry.max")
                    .replace("${retries}", Integer.toString(this.property.getRetries()))
                    .replace("${ip}", this.property.getIp())
                    .replace("${port}", Integer.toString(this.property.getPort())));
            this.retryCountdown = this.property.getRetries();
            return false;
        } else if (!this.property.getRetries().equals(this.retryCountdown)) {
            log.debug(message.getString("fs.ftp.connect.retrying")
                    .replace("${retried}", Integer.toString(this.property.getRetries() - this.retryCountdown))
                    .replace("${retries}", Integer.toString(this.property.getRetries())));
        }
        if (ftpClient == null) {
            ftpClient = new FTPClient();
        }
        try {
            ftpClient.connect(this.property.getIp(), this.property.getPort());
            ftpClient.login(this.property.getUsername(), this.property.getPassword());
            int replyCode = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                log.debug(message.getString("fs.ftp.connect.fail.login")
                        .replace("${ip}", this.property.getIp())
                        .replace("${port}", Integer.toString(this.property.getPort())));
                this.retryCountdown--;
                return connect();
            }
            log.debug(message.getString("fs.ftp.connect.success")
                    .replace("${ip}", this.property.getIp())
                    .replace("${port}", Integer.toString(this.property.getPort())));
            ftpClient.setControlEncoding(this.property.getCharset());
            ftpClient.enterLocalPassiveMode();
            this.retryCountdown = this.property.getRetries();
            return true;
        } catch (ConnectException e) {
            log.error(message.getString("fs.ftp.connect.fail")
                    .replace("${ip}", this.property.getIp())
                    .replace("${port}", Integer.toString(this.property.getPort())));
            this.retryCountdown--;
            return connect();
        } catch (Exception e) {
            log.error(message.getString("fs.ftp.connect.fail")
                    .replace("${ip}", this.property.getIp())
                    .replace("${port}", Integer.toString(this.property.getPort())), e);
            this.retryCountdown--;
            return connect();
        }
    }

    @Override
    public FTPClient getFileSystemHolder() {
        return ftpClient;
    }

    @Override
    public void disconnect() {
        if (null != ftpClient) {
            try {
                ftpClient.disconnect();
                log.debug(message.getString("fs.ftp.disconnect.success"));
            } catch (Exception e) {
                log.error(message.getString("fs.ftp.disconnect.error"), e);
            }
        }
        ftpClient = null;
    }

    @Override
    public List<FileInfo<FTPFile>> list(String path, boolean deepFind, boolean flatPrint, int maxDepth) {
        return list(path, deepFind, flatPrint, maxDepth, 0);
    }

    private List<FileInfo<FTPFile>> list(String path, boolean deepFind, boolean flatPrint, int maxDepth, int nowDepth) {
        if (deepFind && maxDepth >= 0 && nowDepth > maxDepth) {
            return null;
        }
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        if (ftpClient == null || !ftpClient.isConnected() || (this.retryCountdown >= 0 && this.retryCountdown < this.property.getRetries())) {
            if (!connect()) {
                return null;
            }
        }
        if ("/".equals(path)) {
            log.warn(message.getString("fs.ftp.list.warn.from.root"));
        }
        String oriPath = path;
        try {
            path = path.replace("\\", "/");
            path = new String(path.getBytes(this.property.getCharset()), StandardCharsets.ISO_8859_1);
            if (!ftpClient.changeWorkingDirectory(path)) {
                return null;
            }
            FTPFile[] ftpFiles = ftpClient.listFiles();
            List<FileInfo<FTPFile>> files = new ArrayList<>();
            for (FTPFile file : ftpFiles) {
                FileInfo<FTPFile> info = new FileInfo<>();
                info.setAbsolutePath(oriPath);
                info.setFilename(file.getName());
                info.setSize(file.getSize());
                info.setFile(file.isFile());
                info.setDirectory(file.isDirectory());
                info.setOriginalInfo(file);
                files.add(info);
                if (deepFind && file.isDirectory()) {
                    String childPath;
                    if ("/".equals(path)) {
                        childPath = "/" + file.getName();
                    } else if (path.endsWith("/")) {
                        childPath = path + file.getName();
                    } else {
                        childPath = path + "/" + file.getName();
                    }
                    Collection<FileInfo<FTPFile>> list = list(childPath, true, flatPrint, maxDepth, nowDepth + 1);
                    if (list != null) {
                        if (flatPrint) {
                            files.addAll(list);
                        } else {
                            info.setChildren(list);
                        }
                    }
                }
            }
            return files;
        } catch (SocketException e) {
            this.retryCountdown--;
            return list(path, deepFind, flatPrint, maxDepth, nowDepth);
        } catch (Exception e) {
            log.error(message.getString("fs.ftp.list.error")
                    .replace("${path}", path), e);
            return null;
        }
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    public boolean upload(InputStream input, String path, String filename) {
        if (input == null || path == null || path.trim().isEmpty() || filename == null || filename.trim().isEmpty())
            return false;
        if (ftpClient == null || !ftpClient.isConnected() || (this.retryCountdown >= 0 && this.retryCountdown < this.property.getRetries())) {
            if (!connect()) {
                return false;
            }
        }
        String oriPath = path;
        String oriName = filename;
        try {
            path = path.replace("\\", "/");
            path = new String(path.getBytes(this.property.getCharset()), StandardCharsets.ISO_8859_1);
            filename = new String(filename.getBytes(this.property.getCharset()), StandardCharsets.ISO_8859_1);
            if (!ftpClient.changeWorkingDirectory(path)) {
                if (!createDirectory(oriPath)) {
                    return false; // 创建目录失败
                }
            }
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            int len = input.available();
            boolean b = ftpClient.storeFile(filename, input);
            if (b) {
                log.debug(message.getString("fs.ftp.upload.success")
                        .replace("${path}", oriPath)
                        .replace("${filename}", oriName)
                        .replace("${length}", Integer.toString(len)));
            } else {
                log.error(message.getString("fs.ftp.upload.fail")
                        .replace("${path}", oriPath)
                        .replace("${filename}", oriName)
                        .replace("${length}", Integer.toString(len)));
            }
            return b;
        } catch (SocketException e) {
            this.retryCountdown--;
            return upload(input, path, filename);
        } catch (Exception e) {
            log.error(message.getString("fs.ftp.upload.error")
                    .replace("${path}", oriPath)
                    .replace("${filename}", oriName)
                    .replace("${stream}", Integer.toString(input.hashCode())), e);
        }
        return false;
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    public boolean createDirectory(String path) {
        if (path == null || path.trim().isEmpty())
            return false;
        if (ftpClient == null || !ftpClient.isConnected() || (this.retryCountdown >= 0 && this.retryCountdown < this.property.getRetries())) {
            if (!connect()) {
                return false;
            }
        }
        String oriPath = path;
        String d;
        try {
            path = path.replace("\\", "/");
            // 目录编码，解决中文路径问题
            d = new String(path.getBytes(this.property.getCharset()), StandardCharsets.ISO_8859_1);
            path = path.trim();
            String[] arr = path.split("/");
            StringBuilder sbfDir = new StringBuilder();
            // 循环生成子目录
            for (String s : arr) {
                sbfDir.append("/");
                sbfDir.append(s);
                // 目录编码，解决中文路径问题，逐渐将d对象补全至oriDir路径
                d = new String(sbfDir.toString().getBytes(this.property.getCharset()), StandardCharsets.ISO_8859_1);
                // 尝试切入目录
                if (ftpClient.changeWorkingDirectory(d)) {
                    continue;
                }
                if (!ftpClient.makeDirectory(d)) {
                    return false;
                }
            }
            // 将目录切换至指定路径
            return ftpClient.changeWorkingDirectory(d);
        } catch (SocketException e) {
            this.retryCountdown--;
            return createDirectory(path);
        } catch (Exception e) {
            log.error(message.getString("fs.ftp.mkdir.error")
                    .replace("${path}", oriPath), e);
            return false;
        }
    }

    @Override
    public ByteArrayOutputStream downloadStream(String path, String filename) {
        if (path == null || path.trim().isEmpty() || filename == null || filename.trim().isEmpty())
            return null;
        if (ftpClient == null || !ftpClient.isConnected() || (this.retryCountdown >= 0 && this.retryCountdown < this.property.getRetries())) {
            if (!connect()) {
                return null;
            }
        }
        ByteArrayOutputStream outStream = null;
        String oriPath = path;
        String oriName = filename;
        try {
            path = path.replace("\\", "/");
            path = new String(path.getBytes(this.property.getCharset()), StandardCharsets.ISO_8859_1);
            filename = new String(filename.getBytes(this.property.getCharset()), StandardCharsets.ISO_8859_1);
            ftpClient.changeWorkingDirectory(path);
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            try (InputStream inputStream = ftpClient.retrieveFileStream(filename)) {
                if (inputStream != null) {
                    outStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while (-1 != (len = inputStream.read(buffer))) {
                        outStream.write(buffer, 0, len);
                    }
                    if (outStream.size() > 0) {
                        log.debug(message.getString("fs.ftp.download.success")
                                .replace("${length}", Integer.toString(outStream.size())));
                    } else {
                        log.error(message.getString("fs.ftp.download.empty.out")
                                .replace("${path}", oriPath)
                                .replace("${filename}", oriName));
                    }
                } else {
                    log.error(message.getString("fs.ftp.download.empty.in")
                            .replace("${path}", oriPath)
                            .replace("${filename}", oriName));
                }
            }
        } catch (SocketException e) {
            this.retryCountdown--;
            return downloadStream(path, filename);
        } catch (Exception e) {
            log.error(message.getString("fs.ftp.download.error")
                    .replace("${path}", oriPath)
                    .replace("${filename}", oriName), e);
        } finally {
            if (outStream != null) {
                try {
                    ftpClient.completePendingCommand();
                } catch (Exception ignored) {}
            }
        }
        return outStream;
    }

    @Override
    public boolean deleteFile(String path, String filename) {
        if (path == null || path.trim().isEmpty() || filename == null || filename.trim().isEmpty())
            return false;
        if (ftpClient == null || !ftpClient.isConnected() || (this.retryCountdown >= 0 && this.retryCountdown < this.property.getRetries())) {
            if (!connect()) {
                return false;
            }
        }
        boolean flag = false;
        String oriPath = path;
        String oriName = filename;
        try {
            path = path.replace("\\", "/");
            path = new String(path.getBytes(this.property.getCharset()), StandardCharsets.ISO_8859_1);
            filename = new String(filename.getBytes(this.property.getCharset()), StandardCharsets.ISO_8859_1);
            if (ftpClient.changeWorkingDirectory(path)) {
                flag = ftpClient.deleteFile(filename);
            }
            if (flag) {
                log.debug(message.getString("fs.ftp.delete.success")
                        .replace("${path}", oriPath)
                        .replace("${filename}", oriName));
            } else {
                log.debug(message.getString("fs.ftp.delete.error")
                        .replace("${path}", oriPath)
                        .replace("${filename}", oriName));
            }
        } catch (SocketException e) {
            this.retryCountdown--;
            return deleteFile(path, filename);
        } catch (Exception e) {
            log.error(message.getString("fs.ftp.delete.error")
                    .replace("${path}", oriPath)
                    .replace("${filename}", oriName), e);
        }
        return flag;
    }

}
