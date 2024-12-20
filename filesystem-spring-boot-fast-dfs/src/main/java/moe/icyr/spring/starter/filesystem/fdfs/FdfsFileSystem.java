package moe.icyr.spring.starter.filesystem.fdfs;

import com.github.tobato.fastdfs.domain.conn.FdfsConnectionManager;
import com.github.tobato.fastdfs.domain.conn.FdfsConnectionPool;
import com.github.tobato.fastdfs.domain.conn.PooledConnectionFactory;
import com.github.tobato.fastdfs.domain.conn.TrackerConnectionManager;
import com.github.tobato.fastdfs.domain.fdfs.*;
import com.github.tobato.fastdfs.domain.upload.FastFile;
import com.github.tobato.fastdfs.domain.upload.ThumbImage;
import com.github.tobato.fastdfs.exception.FdfsConnectException;
import com.github.tobato.fastdfs.exception.FdfsServerException;
import com.github.tobato.fastdfs.service.*;
import moe.icyr.spring.starter.filesystem.api.FileSystem;
import moe.icyr.spring.starter.filesystem.api.entity.FileInfo;
import moe.icyr.spring.starter.filesystem.api.entity.FileSystemProperty;
import moe.icyr.spring.starter.filesystem.fdfs.entity.FdfsProperty;
import moe.icyr.spring.starter.filesystem.fdfs.entity.Holder;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * FastDFS 连接客户端
 *
 * @author IceLitty
 * @since 1.0
 */
public class FdfsFileSystem extends FileSystem<Holder, com.github.tobato.fastdfs.domain.fdfs.FileInfo> {

    private static final Logger log = LoggerFactory.getLogger(FdfsFileSystem.class);
    private static final ResourceBundle message = ResourceBundle.getBundle("MessageFdfs");

    @SuppressWarnings("FieldCanBeLocal")
    private TrackerConnectionManager trackerConnManager;
    private TrackerClient trackerClient;
    @SuppressWarnings("FieldCanBeLocal")
    private FdfsConnectionManager storageConnManager;
    private FastFileStorageClient storageClient;
    private AppendFileStorageClient appenderStorageClient;

    @SuppressWarnings("FieldCanBeLocal")
    private FdfsProperty property;

    /**
     * 验证配置并创建连接
     *
     * @param property 配置文件
     */
    public FdfsFileSystem(FileSystemProperty property) {
        super(property);
    }

    @Override
    public FdfsProperty validateProperty(FileSystemProperty property) {
        super.validateProperty(property);
        FdfsProperty fdfsProperty;
        if (property instanceof FdfsProperty) {
            fdfsProperty = (FdfsProperty) property;
        } else {
            fdfsProperty = new FdfsProperty(property);
        }
        if (fdfsProperty.getSoTimeout() == null || fdfsProperty.getSoTimeout() < 0) {
            fdfsProperty.setSoTimeout(1500);
        }
        if (fdfsProperty.getConnectTimeout() == null || fdfsProperty.getConnectTimeout() < 0) {
            fdfsProperty.setConnectTimeout(600);
        }
        if (fdfsProperty.getCharset() == null) {
            fdfsProperty.setCharset(StandardCharsets.UTF_8);
        }
        if (fdfsProperty.getThumbImage() == null) {
            fdfsProperty.setThumbImage(new ThumbImage(150, 150));
        }
        if (fdfsProperty.getTrackerList() == null || fdfsProperty.getTrackerList().isEmpty()) {
            throw new IllegalArgumentException(message.getString("fs.fdfs.tracker.list.not.empty"));
        }
        return fdfsProperty;
    }

    @Override
    protected void init(FileSystemProperty property) {
        this.property = (FdfsProperty) property;
        // 连接池配置
        PooledConnectionFactory factory = new PooledConnectionFactory();
        Field[] factoryFields = PooledConnectionFactory.class.getDeclaredFields();
        try {
            for (Field field : factoryFields) {
                switch (field.getName()) {
                    case "soTimeout":
                        field.setAccessible(true);
                        field.set(factory, this.property.getSoTimeout());
                        break;
                    case "connectTimeout":
                        field.setAccessible(true);
                        field.set(factory, this.property.getConnectTimeout());
                        break;
                    case "charset":
                        field.setAccessible(true);
                        field.set(factory, this.property.getCharset());
                        break;
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(message.getString("fs.fdfs.settings.fail"), e);
        }
        FdfsConnectionPool pool = this.property.getPool() == null ? new FdfsConnectionPool(factory) : new FdfsConnectionPool(factory, this.property.getPool());
        // tracker连接池
        trackerConnManager = new TrackerConnectionManager(pool);
        trackerConnManager.setTrackerList(this.property.getTrackerList());
        trackerConnManager.initTracker();
        // tracker客户端
        trackerClient = new DefaultTrackerClient();
        Field[] trackerClientFields = DefaultTrackerClient.class.getDeclaredFields();
        try {
            for (Field field : trackerClientFields) {
                if (field.getType().equals(TrackerConnectionManager.class)) {
                    field.setAccessible(true);
                    field.set(trackerClient, trackerConnManager);
                    break;
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(message.getString("fs.fdfs.settings.fail"), e);
        }
        // storage连接池
        storageConnManager = new FdfsConnectionManager(pool);
        // storage缩略图设置
        DefaultThumbImageConfig defaultThumbImageConfig = new DefaultThumbImageConfig();
        defaultThumbImageConfig.setHeight(this.property.getThumbImage().getHeight());
        defaultThumbImageConfig.setWidth(this.property.getThumbImage().getWidth());
        // storage客户端 & 支持断点续传的storage客户端
        storageClient = new DefaultFastFileStorageClient();
        appenderStorageClient = new DefaultAppendFileStorageClient();
        Field[] storageClientFields0 = DefaultGenerateStorageClient.class.getDeclaredFields();
        Field[] storageClientFields = DefaultFastFileStorageClient.class.getDeclaredFields();
        try {
            for (Field field : storageClientFields0) {
                if (field.getType().equals(TrackerClient.class)) {
                    field.setAccessible(true);
                    field.set(storageClient, trackerClient);
                    field.set(appenderStorageClient, trackerClient);
                } else if (field.getType().equals(FdfsConnectionManager.class)) {
                    field.setAccessible(true);
                    field.set(storageClient, storageConnManager);
                    field.set(appenderStorageClient, storageConnManager);
                }
            }
            for (Field field : storageClientFields) {
                if (field.getType().equals(ThumbImageConfig.class)) {
                    field.setAccessible(true);
                    field.set(storageClient, defaultThumbImageConfig);
                    break;
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(message.getString("fs.fdfs.settings.fail"), e);
        }
    }

    @Override
    protected boolean connect() {
        // 指令测试
        try {
            List<GroupState> groupStates = trackerClient.listGroups();
            StringBuilder p = new StringBuilder();
            if (groupStates != null && !groupStates.isEmpty()) {
                for (GroupState gs : groupStates) {
                    String groupName = gs.getGroupName();
                    long freeMB = gs.getFreeMB();
                    long totalMB = gs.getTotalMB();
                    float percent = (float) freeMB / totalMB;
                    p.append(groupName).append("(").append(percent).append("%,")
                            .append(freeMB).append("MB").append(message.getString("fs.fdfs.word.connect.debug.left")).append(") ");
                }
            }
            log.debug(message.getString("fs.fdfs.connect.debug")
                    .replace("${groupSize}", String.valueOf(groupStates == null ? -1 : groupStates.size()))
                    .replace("${usedPercent}", p.toString()));
            return true;
        } catch (FdfsConnectException e) {
            log.error(message.getString("fs.fdfs.connect.fail"));
            return false;
        } catch (Exception e) {
            log.error(message.getString("fs.fdfs.connect.error"), e);
            return false;
        }
    }

    @Override
    public Holder getFileSystemHolder() {
        return new Holder(trackerConnManager, trackerClient, storageConnManager, storageClient, appenderStorageClient);
    }

    @Override
    public void disconnect() {
    }

    @Override
    public List<FileInfo<com.github.tobato.fastdfs.domain.fdfs.FileInfo>> list(String path, boolean deepFind, boolean flatPrint, int maxDepth) {
        log.error(message.getString("fs.fdfs.ls.not.support"));
        return null;
    }

    /**
     * <p>用于查看文件信息</p>
     * <p><b>不同于其他文件系统，FastDFS不支持自定义目录结构，故没有列出文件信息的功能，
     * 但是为了支持断点续传，需要实现查看服务端文件大小的功能</b></p>
     * @param path 组名+绝对路径
     * @param filename 文件名
     * @return 文件信息
     */
    @Override
    public FileInfo<com.github.tobato.fastdfs.domain.fdfs.FileInfo> peekFile(String path, String filename) {
        if (path == null && filename == null) {
            return null;
        }
        List<String> formats = formatPathAndFilename(path, filename);
        if (formats.get(0) == null || formats.get(1) == null || formats.get(2) == null) {
            log.error(message.getString("fs.fdfs.ls.arguments.invalid")
                    .replace("${path}", String.valueOf(formats.get(0)))
                    .replace("${filename}", String.valueOf(formats.get(1)))
                    .replace("${err}", "FORMAT ERROR"));
            return null;
        }
        String groupName = formats.get(2);
        path = formats.get(0);
        filename = formats.get(1);
        try {
            com.github.tobato.fastdfs.domain.fdfs.FileInfo info = storageClient.queryFileInfo(groupName, path.concat("/").concat(filename));
            FileInfo<com.github.tobato.fastdfs.domain.fdfs.FileInfo> fileInfo = new FileInfo<>();
            fileInfo.setFilename(filename);
            fileInfo.setAbsolutePath("/".concat(groupName).concat("/").concat(path));
            fileInfo.setDirectory(false);
            fileInfo.setFile(true);
            fileInfo.setSize(info.getFileSize());
            fileInfo.setOriginalInfo(info);
            return fileInfo;
        } catch (Exception e) {
            log.error(message.getString("fs.fdfs.ls.error"), e);
            return null;
        }
    }

    /**
     * <p>上传文件</p>
     * <p><b>路径会被忽略，文件名用于识别后缀传输给FastDFS</b></p>
     * @param input 流
     * @param path 路径
     * @param filename 文件名
     * @return 成功与否
     */
    @Override
    public boolean upload(InputStream input, StringBuffer path, StringBuffer filename) {
        if (input == null || path == null || filename == null)
            return false;
        try {
            int suffixIndex = filename.lastIndexOf(".");
            String suffix;
            if (suffixIndex == -1) {
                suffix = "bin";
            } else {
                suffix = filename.substring(suffixIndex + 1);
            }
            Set<MetaData> metaDataSet = new HashSet<>();
            metaDataSet.add(new MetaData("ext_name", suffix));
            metaDataSet.add(new MetaData("file_size", String.valueOf(input.available())));
            metaDataSet.add(new MetaData("height", "0"));
            metaDataSet.add(new MetaData("width", "0"));
            StorePath storePath = storageClient.uploadFile(new FastFile(input, input.available(), suffix, metaDataSet));
            return formatStorePath(path, filename, storePath);
        } catch (Exception e) {
            log.error(message.getString("fs.fdfs.upload.error")
                    .replace("${stream}", Integer.toString(input.hashCode())), e);
            return false;
        }
    }

    @Override
    public boolean upload(InputStream input, String path, String filename) {
        log.error(message.getString("fs.fdfs.upload.not.use.string"));
        return false;
    }

    @Override
    public boolean createDirectory(String path) {
        log.error(message.getString("fs.fdfs.mkdir.not.support"));
        return false;
    }

    /**
     * 下载文件
     * @param path 组名+绝对路径
     * @param filename 文件名
     * @return 流
     */
    @Override
    public ByteArrayOutputStream downloadStream(String path, String filename) {
        if (path == null && filename == null)
            return null;
        try {
            List<String> formats = formatPathAndFilename(path, filename);
            if (formats.get(0) == null || formats.get(1) == null || formats.get(2) == null) {
                log.error(message.getString("fs.fdfs.download.arguments.invalid")
                        .replace("${path}", String.valueOf(formats.get(0)))
                        .replace("${filename}", String.valueOf(formats.get(1)))
                        .replace("${err}", "FORMAT ERROR"));
                return null;
            }
            String groupName = formats.get(2);
            path = formats.get(0);
            filename = formats.get(1);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            storageClient.downloadFile(groupName, path.concat("/").concat(filename), ins -> {
                byte[] bytes = IOUtils.toByteArray(ins);
                outputStream.write(bytes);
                return bytes;
            });
            return outputStream;
        } catch (IllegalArgumentException e) {
            log.error(message.getString("fs.fdfs.download.arguments.invalid")
                    .replace("${path}", String.valueOf(path))
                    .replace("${filename}", String.valueOf(filename))
                    .replace("${err}", e.getMessage()));
            return null;
        } catch (FdfsServerException e) {
            log.error(message.getString("fs.fdfs.download.server.error")
                    .replace("${err}", e.getMessage()));
            return null;
        } catch (Exception e) {
            log.error(message.getString("fs.fdfs.download.error"), e);
            return null;
        }
    }

    /**
     * 删除文件
     * @param path 组名+绝对路径
     * @param filename 文件名
     * @return 成功与否
     */
    @Override
    public boolean deleteFile(String path, String filename) {
        if (path == null && filename == null)
            return false;
        try {
            List<String> formats = formatPathAndFilename(path, filename);
            if (formats.get(0) == null || formats.get(1) == null || formats.get(2) == null) {
                log.error(message.getString("fs.fdfs.delete.arguments.invalid")
                        .replace("${path}", String.valueOf(formats.get(0)))
                        .replace("${filename}", String.valueOf(formats.get(1)))
                        .replace("${err}", "FORMAT ERROR"));
                return false;
            }
            String groupName = formats.get(2);
            path = formats.get(0);
            filename = formats.get(1);
            storageClient.deleteFile(groupName, path.concat("/").concat(filename));
            return true;
        } catch (IllegalArgumentException e) {
            log.error(message.getString("fs.fdfs.delete.arguments.invalid")
                    .replace("${path}", String.valueOf(path))
                    .replace("${filename}", String.valueOf(filename))
                    .replace("${err}", e.getMessage()));
            return false;
        } catch (FdfsServerException e) {
            log.error(message.getString("fs.fdfs.delete.server.error")
                    .replace("${err}", e.getMessage()));
            return false;
        } catch (Exception e) {
            log.error(message.getString("fs.fdfs.delete.error"), e);
            return false;
        }
    }

    /**
     * <p>格式化路径</p>
     * <p>支持以下流程（完整路径：组名+路径+文件名）：</p>
     * <p><ul>
     *     <li>路径：完整路径，文件名：空</li>
     *     <li>路径：空，文件名：完整路径</li>
     *     <li>路径：组名+路径，文件名：文件名</li>
     *     <li>路径：组名，文件名：路径+文件名</li>
     * </ul></p>
     * @param path 原路径参数
     * @param filename 原文件名参数
     * @return 0-路径，1-文件名，2-组名
     */
    private List<String> formatPathAndFilename(String path, String filename) {
        List<String> l = new ArrayList<>();
        if (path == null && filename == null) {
            l.add(null);
            l.add(null);
            l.add(null);
            return l;
        }
        // 格式化路径和文件名
        if (path != null && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (filename != null && filename.startsWith("/")) {
            filename = filename.substring(1);
        }
        if (path != null && filename == null) {
            // 全部路径在path中
            int suffixIndex = path.lastIndexOf("/");
            if (suffixIndex == -1) {
                l.add(path);
                l.add(null);
                l.add(null);
                return l;
            } else {
                filename = path.substring(suffixIndex + 1);
                path = path.substring(0, suffixIndex);
            }
        } else if (path == null) {
            // 全部路径在filename中
            int suffixIndex = filename.lastIndexOf("/");
            if (suffixIndex == -1) {
                l.add(null);
                l.add(filename);
                l.add(null);
                return l;
            } else {
                path = filename.substring(0, suffixIndex);
                filename = filename.substring(suffixIndex + 1);
            }
        } else {
            // 两者皆有
            if (filename.contains("/")) {
                path = path.concat("/").concat(filename);
                int suffixIndex = path.lastIndexOf("/");
                filename = path.substring(suffixIndex + 1);
                path = path.substring(0, suffixIndex);
            }
        }
        // 查找GroupName
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        int prefixIndex = path.indexOf("/");
        String groupName;
        if (prefixIndex == -1) {
            l.add(path);
            l.add(filename);
            l.add(null);
            return l;
        } else {
            groupName = path.substring(0, prefixIndex);
            path = path.substring(prefixIndex + 1);
        }
        l.add(path);
        l.add(filename);
        l.add(groupName);
        return l;
    }

    private boolean formatStorePath(StringBuffer path, StringBuffer filename, StorePath storePath) {
        String fullPath = storePath.getFullPath();
        int filenameIndex = fullPath.lastIndexOf("/");
        if (filenameIndex == -1) {
            log.error(message.getString("fs.fdfs.upload.path.cant.analyze")
                    .replace("${path}", fullPath));
            return false;
        }
        String prefixPath = fullPath.substring(0, filenameIndex);
        String suffixFilename = fullPath.substring(filenameIndex + 1);
        path.setLength(0);
        path.append("/").append(prefixPath);
        filename.setLength(0);
        filename.append(suffixFilename);
        return true;
    }

    /**
     * <p>支持断点续传的上传功能</p>
     * <p><b>若是首次上传：</b>路径必须传组名，文件名用于识别文件后缀传输给FastDFS。</p>
     * <p><b>若是断点续传：</b>路径需要传带组名的完整地址，文件名可附加于路径对象末尾或在文件名对象上。
     * <b>可直接将首次上传后的两个StringBuffer对象传入后续的断点续传。</b></p>
     * @param input 当前块的输入流
     * @param path 首次上传模式：组名，断点续传模式：组名+文件路径
     * @param filename 文件名
     * @param fileSize 当前块大小
     * @param fileOffset 从文件该偏移量处继续上传
     * @return 成功与否
     */
    @Override
    public boolean appenderUpload(InputStream input, StringBuffer path, StringBuffer filename, long fileSize, long fileOffset) {
        if (input == null || path == null || filename == null || fileSize < 0 || fileOffset < 0)
            return false;
        try {
            if (fileOffset == 0) {
                // 首次上传
                int suffixIndex = filename.lastIndexOf(".");
                String suffix;
                if (suffixIndex == -1) {
                    suffix = "bin";
                } else {
                    suffix = filename.substring(suffixIndex + 1);
                }
                StorePath storePath = appenderStorageClient.uploadAppenderFile(path.toString(), input, fileSize, suffix);
                if (!formatStorePath(path, filename, storePath))
                    return false;
                List<String> l = formatPathAndFilename(path.toString(), filename.toString());
                Set<MetaData> metaDataSet = new HashSet<>();
                metaDataSet.add(new MetaData("ext_name", suffix));
                metaDataSet.add(new MetaData("file_size", String.valueOf(fileSize)));
                metaDataSet.add(new MetaData("height", "0"));
                metaDataSet.add(new MetaData("width", "0"));
                appenderStorageClient.overwriteMetadata(l.get(2), l.get(0).concat("/").concat(l.get(1)), metaDataSet);
            } else {
                // 断点续传
                String pathStr = path.charAt(0) == '/' ? path.substring(1) : path.toString();
                int prefixIndex = pathStr.indexOf("/");
                if (prefixIndex == -1) {
                    log.error(message.getString("fs.fdfs.upload.arguments.invalid")
                            .replace("${path}", path.toString())
                            .replace("${filename}", filename.toString())
                            .replace("${err}", "\"/\" NOT FOUND"));
                    return false;
                }
                String groupName = pathStr.substring(0, prefixIndex);
                pathStr = pathStr.substring(prefixIndex + 1);
                if (filename.length() > 0) {
                    pathStr = pathStr.concat("/").concat(filename.toString());
                }
                FileInfo<com.github.tobato.fastdfs.domain.fdfs.FileInfo> fileInfo = peekFile(groupName, pathStr);
                Set<MetaData> metaData = new HashSet<>();
                metaData.add(new MetaData("file_size", String.valueOf(fileInfo.getSize() + fileSize)));
                appenderStorageClient.modifyFile(groupName, pathStr, input, fileSize, fileOffset);
                appenderStorageClient.mergeMetadata(groupName, pathStr, metaData);
            }
            return true;
        } catch (Exception e) {
            log.error(message.getString("fs.fdfs.upload.error")
                    .replace("${stream}", Integer.toString(input.hashCode())), e);
            return false;
        }
    }

}
