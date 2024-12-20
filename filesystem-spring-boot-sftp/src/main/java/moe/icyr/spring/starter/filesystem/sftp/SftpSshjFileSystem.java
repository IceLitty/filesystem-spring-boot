package moe.icyr.spring.starter.filesystem.sftp;

import moe.icyr.spring.starter.filesystem.api.FileSystem;
import moe.icyr.spring.starter.filesystem.api.entity.FileInfo;
import moe.icyr.spring.starter.filesystem.api.entity.FileSystemProperty;
import moe.icyr.spring.starter.filesystem.sftp.entity.InMemoryFile;
import moe.icyr.spring.starter.filesystem.sftp.entity.SftpProperty;
import net.schmizz.keepalive.KeepAliveProvider;
import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.FileMode;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.xfer.InMemorySourceFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.PublicKey;
import java.util.*;

/**
 * SFTP文件服务连接器
 *
 * @author IceLitty
 * @since 1.0
 */
public class SftpSshjFileSystem extends FileSystem<SFTPClient, RemoteResourceInfo> {

    private static final Logger log = LoggerFactory.getLogger(SftpSshjFileSystem.class);
    private static final ResourceBundle message = ResourceBundle.getBundle("MessageSftp");

    private SftpProperty property;

    private SSHClient sshClient;
    private SFTPClient sftpClient;

    /**
     * 验证配置并创建连接
     *
     * @param property 配置文件
     */
    public SftpSshjFileSystem(FileSystemProperty property) {
        super(property);
    }

    @Override
    public SftpProperty validateProperty(FileSystemProperty property) {
        super.validateProperty(property);
        SftpProperty sftpProperty;
        if (property instanceof SftpProperty) {
            sftpProperty = (SftpProperty) property;
        } else {
            sftpProperty = new SftpProperty(property);
        }
        if (sftpProperty.getKeepAliveSecond() == null || sftpProperty.getKeepAliveSecond() < 0) {
            sftpProperty.setKeepAliveSecond(0);
        }
        if (sftpProperty.getPrivateKey() != null) {
            if (!new File(sftpProperty.getPrivateKey()).exists()) {
                throw new IllegalArgumentException(message.getString("fs.sftp.connect.fail.private.key.not.found")
                        .replace("${privKeyPath}", sftpProperty.getPrivateKey()));
            }
        }
        return sftpProperty;
    }

    @Override
    protected void init(FileSystemProperty property) {
        this.property = (SftpProperty) property;
    }

    @Override
    protected boolean connect() {
        if (sshClient == null || !sshClient.isConnected()) {
            DefaultConfig config = new DefaultConfig();
            config.setKeepAliveProvider(KeepAliveProvider.KEEP_ALIVE);
            sshClient = new SSHClient(config);
            sshClient.addHostKeyVerifier(new HostKeyVerifier() {
                @Override
                public boolean verify(String ip, int port, PublicKey publicKey) {
                    return true;
                }
                @Override
                public List<String> findExistingAlgorithms(String ip, int port) {
                    return null;
                }
            });
            try {
                sshClient.connect(this.property.getIp(), this.property.getPort());
            } catch (Exception e) {
                log.error(message.getString("fs.sftp.connect.fail")
                        .replace("${ip}", this.property.getIp())
                        .replace("${port}", Integer.toString(this.property.getPort())), e);
                return false;
            }
            if (this.property.getKeepAliveSecond() > 0) {
                try {
                    sshClient.getConnection().getKeepAlive().setKeepAliveInterval(this.property.getKeepAliveSecond());
                } catch (Exception e) {
                    log.error(message.getString("fs.sftp.connect.fail.setting.heartbeat")
                            .replace("${value}", Integer.toString(this.property.getKeepAliveSecond())), e);
                    return false;
                }
            }
            if (this.property.getPrivateKey() != null && !this.property.getPrivateKey().trim().isEmpty()) {
                try {
                    KeyProvider keyProvider;
                    if (this.property.getPassword() != null && !this.property.getPassword().trim().isEmpty()) {
                        keyProvider = sshClient.loadKeys(this.property.getPrivateKey(), this.property.getPassword());
                    } else {
                        keyProvider = sshClient.loadKeys(this.property.getPrivateKey());
                    }
                    sshClient.authPublickey(this.property.getUsername(), keyProvider);
                } catch (Exception e) {
                    log.error(message.getString("fs.sftp.connect.fail.private.key")
                            .replace("${ip}", this.property.getIp())
                            .replace("${port}", Integer.toString(this.property.getPort()))
                            .replace("${username}", this.property.getUsername()), e);
                    return false;
                }
            } else if (this.property.getPassword() != null && !this.property.getPassword().trim().isEmpty()) {
                try {
                    sshClient.authPassword(this.property.getUsername(), this.property.getPassword());
                } catch (Exception e) {
                    log.error(message.getString("fs.sftp.connect.fail.login")
                            .replace("${ip}", this.property.getIp())
                            .replace("${port}", Integer.toString(this.property.getPort()))
                            .replace("${username}", this.property.getUsername()), e);
                    return false;
                }
            } else {
                log.error(message.getString("fs.sftp.connect.fail.not.support.anonymous"));
                return false;
            }
            try {
                sftpClient = sshClient.newSFTPClient();
            } catch (IOException e) {
                log.error(message.getString("fs.sftp.connect.fail.sftp.channel"), e);
                return false;
            }
        }
        return true;
    }

    @Override
    public SFTPClient getFileSystemHolder() {
        return sftpClient;
    }

    @Override
    public void disconnect() {
        try {
            if (sftpClient != null) {
                sftpClient.close();
                sftpClient = null;
            }
            if (sshClient != null) {
                if (sshClient.isConnected()) {
                    sshClient.close();
                }
                sshClient = null;
            }
        } catch (Exception e) {
            log.warn(message.getString("fs.sftp.disconnect.fail"), e);
        }
    }

    @Override
    public List<FileInfo<RemoteResourceInfo>> list(String path, boolean deepFind, boolean flatPrint, int maxDepth) {
        return list(path, deepFind, flatPrint, maxDepth, 0);
    }

    private List<FileInfo<RemoteResourceInfo>> list(String path, boolean deepFind, boolean flatPrint, int maxDepth, int nowDepth) {
        if (deepFind && maxDepth >= 0 && nowDepth > maxDepth) {
            return null;
        }
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        path = path.replace("\\", "/");
        if (sftpClient == null || sshClient == null || !sshClient.isConnected()) {
            if (!connect()) {
                return null;
            }
        }
        if ("/".equals(path)) {
            log.warn(message.getString("fs.sftp.list.warn.from.root"));
        }
        try {
            List<RemoteResourceInfo> ls = sftpClient.ls(path);
            List<FileInfo<RemoteResourceInfo>> files = new ArrayList<>();
            for (RemoteResourceInfo file : ls) {
                if (".".equals(file.getName()) || "..".equals(file.getName())) {
                    continue;
                }
                FileInfo<RemoteResourceInfo> info = new FileInfo<>();
                info.setAbsolutePath(path);
                info.setFilename(file.getName());
                info.setSize(file.getAttributes().getSize());
                info.setFile(file.isRegularFile());
                info.setDirectory(file.isDirectory());
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
                    Collection<FileInfo<RemoteResourceInfo>> list = list(childPath, true, flatPrint, maxDepth, nowDepth + 1);
                    if (flatPrint) {
                        files.addAll(list);
                    } else {
                        info.setChildren(list);
                    }
                }
            }
            return files;
        } catch (Exception e) {
            log.error(message.getString("fs.sftp.list.error")
                    .replace("${path}", path), e);
            return null;
        }
    }

    @Override
    public boolean upload(InputStream input, String path, String filename) {
        if (input == null || path == null || path.trim().isEmpty() || filename == null || filename.trim().isEmpty())
            return false;
        path = path.replace("\\", "/");
        if (sftpClient == null || sshClient == null || !sshClient.isConnected()) {
            if (!connect()) {
                return false;
            }
        }
        boolean cd = createDirectory(path);
        if (!cd) {
            return false;
        }
        try {
            int len = input.available();
            sftpClient.getFileTransfer().upload(new InMemorySourceFile() {
                @Override
                public String getName() {
                    return filename;
                }
                @Override
                public long getLength() {
                    return len;
                }
                @Override
                public InputStream getInputStream() {
                    return input;
                }
            }, path);
            log.debug(message.getString("fs.sftp.upload.success")
                    .replace("${path}", path)
                    .replace("${filename}", filename)
                    .replace("${length}", Integer.toString(len)));
            return true;
        } catch (Exception e) {
            log.error(message.getString("fs.sftp.upload.error")
                    .replace("${path}", path)
                    .replace("${filename}", filename)
                    .replace("${stream}", Integer.toString(input.hashCode())), e);
            return false;
        }
    }

    @Override
    public boolean createDirectory(String path) {
        if (path == null || path.trim().isEmpty())
            return false;
        path = path.replace("\\", "/");
        if (sftpClient == null || sshClient == null || !sshClient.isConnected()) {
            if (!connect()) {
                return false;
            }
        }
        if ("/".equals(path)) {
            return true;
        }
        String[] paths = path.split("/");
        try {
            StringBuilder pathBuilder = new StringBuilder("/");
            for (String dir : paths) {
                if (dir.trim().isEmpty()) {
                    continue;
                }
                Boolean exists = false;
                List<RemoteResourceInfo> ls = sftpClient.ls(pathBuilder.toString());
                for (RemoteResourceInfo file : ls) {
                    if (".".equals(file.getName()) || "..".equals(file.getName())) {
                        continue;
                    }
                    if (dir.equals(file.getName())) {
                        exists = true;
                        if (!file.isDirectory() && file.getAttributes().getType() != FileMode.Type.SYMLINK) {
                            exists = null;
                        }
                        break;
                    }
                }
                if (exists == null) {
                    log.error(message.getString("fs.sftp.mkdir.fail.exists.not.directory")
                            .replace("${path}", pathBuilder.toString()));
                    return false;
                }
                pathBuilder.append(dir).append("/");
                if (!exists) {
                    sftpClient.mkdir(pathBuilder.toString());
                }
            }
            return true;
        } catch (Exception e) {
            log.error(message.getString("fs.sftp.mkdir.error")
                    .replace("${path}", path), e);
            return false;
        }
    }

    @Override
    public ByteArrayOutputStream downloadStream(String path, String filename) {
        if (path == null || path.trim().isEmpty() || filename == null || filename.trim().isEmpty())
            return null;
        path = path.replace("\\", "/");
        if (sftpClient == null || sshClient == null || !sshClient.isConnected()) {
            if (!connect()) {
                return null;
            }
        }
        try {
            String filePath;
            if ("/".equals(path)) {
                filePath = "/" + filename;
            } else if (path.endsWith("/")) {
                filePath = path + filename;
            } else {
                filePath = path + "/" + filename;
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            sftpClient.getFileTransfer().download(filePath, new InMemoryFile(outputStream));
            return outputStream;
        } catch (Exception e) {
            log.error(message.getString("fs.sftp.download.error")
                    .replace("${path}", path)
                    .replace("${filename}", filename), e);
            return null;
        }
    }

    @Override
    public boolean deleteFile(String path, String filename) {
        if (path == null || path.trim().isEmpty() || filename == null || filename.trim().isEmpty())
            return false;
        path = path.replace("\\", "/");
        if (sftpClient == null || sshClient == null || !sshClient.isConnected()) {
            if (!connect()) {
                return false;
            }
        }
        try {
            String filePath;
            if ("/".equals(path)) {
                filePath = "/" + filename;
            } else if (path.endsWith("/")) {
                filePath = path + filename;
            } else {
                filePath = path + "/" + filename;
            }
            sftpClient.rm(filePath);
            log.debug(message.getString("fs.sftp.delete.success")
                    .replace("${path}", path)
                    .replace("${filename}", filename));
            return true;
        } catch (Exception e) {
            log.error(message.getString("fs.sftp.delete.error")
                    .replace("${path}", path)
                    .replace("${filename}", filename), e);
            return false;
        }
    }

}
