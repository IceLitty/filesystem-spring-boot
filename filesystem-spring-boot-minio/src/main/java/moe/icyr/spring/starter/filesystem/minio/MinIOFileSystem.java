package moe.icyr.spring.starter.filesystem.minio;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import moe.icyr.spring.starter.filesystem.api.FileSystem;
import moe.icyr.spring.starter.filesystem.api.entity.FileInfo;
import moe.icyr.spring.starter.filesystem.api.entity.FileSystemProperty;
import moe.icyr.spring.starter.filesystem.minio.entity.MinIOProperty;
import moe.icyr.spring.starter.filesystem.minio.entity.UploadReqParams;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * MinIO文件服务连接器
 *
 * @author IceLitty
 * @since 1.3
 */
public class MinIOFileSystem extends FileSystem<MinioClient, Item> {

    private static final Logger log = LoggerFactory.getLogger(MinIOFileSystem.class);
    private static final ResourceBundle message = ResourceBundle.getBundle("MessageMinio");
    private static final Pattern protocolPrefix = Pattern.compile("^\\w+://");

    private MinIOProperty property;

    private MinioClient minioClient;

    /**
     * 验证配置并创建连接
     *
     * @param property 配置文件
     */
    public MinIOFileSystem(FileSystemProperty property) {
        super(property);
    }

    @Override
    public FileSystemProperty validateProperty(FileSystemProperty property) {
        super.validateProperty(property);
        MinIOProperty minioProperty;
        if (property instanceof MinIOProperty) {
            minioProperty = (MinIOProperty) property;
        } else {
            minioProperty = new MinIOProperty(property);
        }
        if (!minioProperty.getIp().contains(":") && minioProperty.getPort() != 0) {
            minioProperty.setIp(minioProperty.getIp() + ":" + minioProperty.getPort());
        }
        if (!protocolPrefix.matcher(minioProperty.getIp()).find()) {
            //noinspection HttpUrlsUsage
            minioProperty.setIp("http://" + minioProperty.getIp());
        }
        return minioProperty;
    }

    @Override
    protected void init(FileSystemProperty property) {
        this.property = (MinIOProperty) property;
    }

    /**
     * 指定用于连接的HttpClient
     *
     * @param httpClient OkHttpClient
     * @return 连接成功与否
     */
    public boolean connectWithCustomHttpClient(OkHttpClient httpClient) {
        if (this.minioClient == null) {
            return connect(httpClient);
        } else {
            log.error(message.getString("fs.minio.connect.already.exists"));
            return false;
        }
    }

    @Override
    protected boolean connect() {
        if (!property.isCustomHttpClient()) {
            return connect(null);
        }
        return true;
    }

    private boolean connect(OkHttpClient okHttpClient) {
        try {
            MinioClient.Builder builder = MinioClient.builder()
                    .endpoint(this.property.getIp())
                    .credentials(this.property.getUsername(), this.property.getPassword());
            if (okHttpClient != null) {
                builder.httpClient(okHttpClient);
            }
            minioClient = builder.build();
            return true;
        } catch (Exception e) {
            log.error(message.getString("fs.minio.connect.error")
                    .replace("${endpoint}", String.valueOf(this.property.getIp())), e);
            return false;
        }
    }

    @Override
    public MinioClient getFileSystemHolder() {
        return minioClient;
    }

    @Override
    public void disconnect() {
        if (minioClient != null) {
            try {
                minioClient.close();
            } catch (Exception e) {
                log.error(message.getString("fs.minio.disconnect.error"), e);
            }
        }
    }

    /**
     * 从接口规范的path和filename字段提取桶名称和对象路径（包含文件(夹)名）
     * 返回名称均不以/开头和/结尾，对象路径可为null
     *
     * @param path     path
     * @param filename filename
     * @return String[桶名称,对象路径]
     */
    private String[] extractBucketAndObjectNameFromPathAndFilename(String path, String filename) {
        String combine = path == null ? "" : (path.startsWith("/") ? path.substring(1) : path);
        if (filename != null) {
            if (combine.endsWith("/") && filename.startsWith("/")) {
                combine = combine.substring(0, combine.length() - 1);
            }
            combine += (combine.endsWith("/") || filename.startsWith("/") ? "" : "/") + (filename.endsWith("/") ? filename.substring(0, filename.length() - 1) : filename);
        }
        String bucketName = null;
        StringBuilder objectName = null;
        for (String s : combine.split("/")) {
            if (bucketName == null) {
                if (!s.isEmpty()) {
                    bucketName = s;
                }
            } else if (objectName == null) {
                objectName = new StringBuilder(s);
            } else {
                objectName.append("/").append(s);
            }
        }
        return new String[]{bucketName, objectName == null ? null : objectName.toString()};
    }

    /**
     * 检测桶是否存在
     * @param bucketName 桶名称
     * @return 是否存在，null为检测异常
     */
    public Boolean checkBucketExists(String bucketName) {
        if (minioClient == null) {
            log.error(message.getString("fs.minio.not.connected"));
            return null;
        }
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .extraHeaders(this.property.getApiHeaders())
                    .build());
        } catch (Exception e) {
            log.error(message.getString("fs.minio.bucket.exists.check.error"), e);
            return null;
        }
    }

    /**
     * 列出文件(夹)
     * <p style="color:orange">flatPrint若为true，则与其他文件系统不同，返回内容不包含文件夹</p>
     * <p style="color:orange">数据条数过多时请注意内存用量</p>
     *
     * @param path 桶名称/对象路径
     * @param deepFind 是否读取子文件
     * @param flatPrint 是否扁平化输出
     * @param maxDepth 当deepFind为true时，限制最大深度，需&gt;=0，&lt;0为不限制
     * @return 文件(夹)信息列表，若没有这个目录、文件服务器连接失败、或其他异常则返回NULL
     */
    @Override
    public List<FileInfo<Item>> list(String path, boolean deepFind, boolean flatPrint, int maxDepth) {
        return list(path, deepFind, flatPrint, maxDepth, false);
    }

    /**
     * 列出文件(夹)（包含文件版本号）
     * <p style="color:orange">flatPrint若为true，则与其他文件系统不同，返回内容不包含文件夹</p>
     * <p style="color:orange">数据条数过多时请注意内存用量</p>
     *
     * @param path 桶名称/对象路径
     * @param deepFind 是否读取子文件
     * @param flatPrint 是否扁平化输出
     * @param maxDepth 当deepFind为true时，限制最大深度，需&gt;=0，&lt;0为不限制
     * @param needVersions 需要查询文件版本号，以用于针对文件版本号进行调用
     * @return 文件(夹)信息列表，若没有这个目录、文件服务器连接失败、或其他异常则返回NULL
     */
    public List<FileInfo<Item>> list(String path, boolean deepFind, boolean flatPrint, int maxDepth, boolean needVersions) {
        if (path == null || path.isEmpty()) {
            List<FileInfo<Item>> files = new ArrayList<>();
            try {
                for (Bucket bucket : minioClient.listBuckets()) {
                    FileInfo<Item> fileInfo = new FileInfo<>();
                    fileInfo.setFilename(bucket.name());
                    fileInfo.setFile(false);
                    fileInfo.setDirectory(true);
                    files.add(fileInfo);
                }
            } catch (Exception e) {
                log.error(message.getString("fs.minio.list.bucket.error"), e);
            }
            return files;
        } else {
            return list(path, deepFind, flatPrint, maxDepth, 0, needVersions);
        }
    }

    private List<FileInfo<Item>> list(String path, boolean deepFind, boolean flatPrint, int maxDepth, int nowDepth, boolean needVersions) {
        if (deepFind && maxDepth >= 0 && nowDepth > maxDepth) {
            return null;
        }
        if (minioClient == null) {
            log.error(message.getString("fs.minio.not.connected"));
            return null;
        }
        List<FileInfo<Item>> files = new ArrayList<>();
        String[] extract = extractBucketAndObjectNameFromPathAndFilename(path, null);
        String bucketName = extract[0];
        String objectName = extract[1];
        try {
            ListObjectsArgs.Builder builder = ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .prefix((objectName == null || objectName.isEmpty()) ? "" : (objectName + "/"))
                    .recursive(deepFind && flatPrint) // true递归 false文件夹形式
                    .extraHeaders(this.property.getApiHeaders());
            if (needVersions) {
                builder.includeVersions(true);
            }
            for (Result<Item> object : minioClient.listObjects(builder
                    .build())) {
                Item item = object.get();
                if (needVersions && !item.isLatest()) {
                    continue;
                }
                // 排除文件夹自身
                if (objectName != null && (objectName + "/").equals(item.objectName())) {
                    continue;
                }
                FileInfo<Item> fileInfo = new FileInfo<>();
                String fullPath = bucketName + (objectName == null || objectName.isEmpty() ? "" : ("/" + objectName)) + "/" + (
                        objectName != null && item.objectName().startsWith(objectName) ? item.objectName().substring(objectName.length() + 1) : item.objectName()
                );
                if (fullPath.endsWith("/")) {
                    fullPath = fullPath.substring(0, fullPath.length() - 1);
                }
                fileInfo.setAbsolutePath(fullPath.substring(0, fullPath.lastIndexOf("/")));
                fileInfo.setFilename(fullPath.substring(fullPath.lastIndexOf("/") + 1));
                fileInfo.setSize(item.size());
                fileInfo.setFile(!item.isDir());
                fileInfo.setDirectory(item.isDir());
                fileInfo.setOriginalInfo(item);
                files.add(fileInfo);
                if (!flatPrint && deepFind && item.isDir()) {
                    Collection<FileInfo<Item>> dir = list(fullPath, deepFind, flatPrint, maxDepth, nowDepth + 1, needVersions);
                    if (dir != null) {
                        fileInfo.setChildren(dir);
                    }
                }
            }
        } catch (Exception e) {
            log.error(message.getString("fs.minio.list.error")
                    .replace("${bucketName}", String.valueOf(bucketName))
                    .replace("${objectName}", String.valueOf(objectName)), e);
        }
        return files;
    }

    @Override
    public boolean upload(File file, StringBuffer path, StringBuffer filename) {
        return upload(file, path.toString(), filename.toString());
    }

    @Override
    public boolean upload(File file, String path, String filename) {
        return uploadWithMetadata(file, path, filename, UploadReqParams.builder().metaData(this.property.getApiUserMetadata()).build());
    }

    @Override
    public boolean upload(InputStream input, String path, String filename) {
        return uploadWithMetadata(input, path, filename, UploadReqParams.builder().metaData(this.property.getApiUserMetadata()).build());
    }

    /**
     * <p>上传文件，通过文件对象</p>
     * <p>提供给特殊需求使用的方法</p>
     * @param file 需要上传的文件
     * @param path 目标文件绝对路径
     * @param filename 目标文件名称
     * @param params 额外上传参数
     * @return 上传成功与否
     */
    public boolean uploadWithMetadata(File file, String path, String filename, UploadReqParams params) {
        if (minioClient == null) {
            log.error(message.getString("fs.minio.not.connected"));
            return false;
        }
        String[] extract = extractBucketAndObjectNameFromPathAndFilename(path, filename);
        String bucketName = extract[0];
        String objectName = extract[1];
        try {
            UploadObjectArgs.Builder builder = UploadObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .filename(file.getCanonicalPath())
                    .headers(this.property.getApiHeaders());
            if (params != null) {
                if (params.getMetaData() != null) {
                    builder.userMetadata(params.getMetaData());
                }
            }
            minioClient.uploadObject(builder.build());
            log.debug(message.getString("fs.minio.upload.success")
                    .replace("${bucketName}", String.valueOf(bucketName))
                    .replace("${objectName}", String.valueOf(objectName))
                    .replace("${length}", Long.toString(file.length())));
            return true;
        } catch (Exception e) {
            log.error(message.getString("fs.minio.upload.error")
                    .replace("${bucketName}", String.valueOf(bucketName))
                    .replace("${objectName}", String.valueOf(objectName))
                    .replace("${stream}", Integer.toString(file.hashCode())), e);
            return false;
        }
    }

    /**
     * 上传文件，通过Base64
     * @param base64 需要上传的Base64
     * @param path 目标文件绝对路径
     * @param filename 目标文件名称
     * @param params 额外上传参数
     * @return 上传成功与否
     */
    public boolean uploadWithMetadata(String base64, String path, String filename, UploadReqParams params) {
        return uploadWithMetadata(super._base64ToBytes(base64, path, filename), path, filename, params);
    }

    /**
     * 上传文件，通过字节
     * @param bytes 需要上传的字节
     * @param path 目标文件绝对路径
     * @param filename 目标文件名称
     * @param params 额外上传参数
     * @return 上传成功与否
     */
    public boolean uploadWithMetadata(byte[] bytes, String path, String filename, UploadReqParams params) {
        return uploadWithMetadata(new ByteArrayInputStream(bytes), path, filename, params);
    }

    /**
     * 上传文件，通过输入流
     * @param input 需要上传的流
     * @param path 目标文件绝对路径
     * @param filename 目标文件名称
     * @param params 额外上传参数
     * @return 上传成功与否
     */
    public boolean uploadWithMetadata(InputStream input, String path, String filename, UploadReqParams params) {
        if (minioClient == null) {
            log.error(message.getString("fs.minio.not.connected"));
            return false;
        }
        String[] extract = extractBucketAndObjectNameFromPathAndFilename(path, filename);
        String bucketName = extract[0];
        String objectName = extract[1];
        try {
            int len = input.available();
            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(input, len, params != null && params.getPartSize() > -1 ? params.getPartSize() : this.property.getUploadDefaultPartSize())
                    .headers(this.property.getApiHeaders())
                    .userMetadata(this.property.getApiUserMetadata());
            if (params != null) {
                if (params.getMetaData() != null) {
                    builder.userMetadata(params.getMetaData());
                }
            }
            minioClient.putObject(builder.build());
            log.debug(message.getString("fs.minio.upload.success")
                    .replace("${bucketName}", String.valueOf(bucketName))
                    .replace("${objectName}", String.valueOf(objectName))
                    .replace("${length}", Integer.toString(len)));
            return true;
        } catch (Exception e) {
            log.error(message.getString("fs.minio.upload.error")
                    .replace("${bucketName}", String.valueOf(bucketName))
                    .replace("${objectName}", String.valueOf(objectName))
                    .replace("${stream}", Integer.toString(input.hashCode())), e);
            return false;
        }
    }

    /**
     * 根据给定路径创建至目录
     * <p style="color:orange">警告：该方法会额外创建一个视为文件夹的文件（在文件夹视图中会显示为：桶/文件夹/文件，在平铺视图中会显示为：桶/文件），MinIO API未提供仅创建文件夹的接口，建议直接上传文件使其自动生成文件目录结构</p>
     *
     * @param path 绝对路径
     * @return 创建成功与否
     */
    @Override
    public boolean createDirectory(String path) {
        String[] extract = extractBucketAndObjectNameFromPathAndFilename(path, null);
        String bucketName = extract[0];
        String objectName = extract[1];
        if (bucketName == null) {
            log.error(message.getString("fs.minio.empty.bucket.name"));
            return false;
        }
        Boolean b = checkBucketExists(bucketName);
        if (b == null) {
            return false;
        } else if (!b) {
            if (this.property.isCanCreateBucket()) {
                try {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(path)
                            .build());
                    return true;
                } catch (Exception e) {
                    log.error(message.getString("fs.minio.create.bucket.error")
                            .replace("${bucketName}", String.valueOf(bucketName)), e);
                    return false;
                }
            } else {
                log.error(message.getString("fs.minio.create.bucket.reject")
                        .replace("${alias}", this.property.getAlias()));
                return false;
            }
        } else {
            if (objectName == null || objectName.isEmpty()) {
                return true;
            } else {
                try {
                    try {
                        minioClient.statObject(StatObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .extraHeaders(this.property.getApiHeaders())
                                .build());
                        return true;
                    } catch (ErrorResponseException ignored) {
                    }
                    // not exist
                    minioClient.putObject(PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName + "/")
                            .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                            .extraHeaders(this.property.getApiHeaders())
                            .userMetadata(this.property.getApiUserMetadata())
                            .build());
                    return true;
                } catch (Exception e) {
                    log.error(message.getString("fs.minio.mkdir.error")
                            .replace("${bucketName}", String.valueOf(bucketName))
                            .replace("${objectName}", String.valueOf(objectName)), e);
                    return false;
                }
            }
        }
    }

    /**
     * 下载为临时文件
     * @param path 绝对路径
     * @param filename 文件名
     * @return 文件
     */
    @Override
    public File downloadFile(String path, String filename) {
        File dest;
        try {
            String destFilename;
            String destFileSuffix;
            if (filename == null) {
                destFilename = "FileSystemUtilTempDownload";
                destFileSuffix = ".bin";
            } else {
                String[] split = filename.split("/");
                String name;
                if (split.length > 1) {
                    name = split[split.length - 1];
                } else {
                    name = filename;
                }
                if (name.contains(".")) {
                    destFilename = name.substring(0, name.lastIndexOf("."));
                    destFileSuffix = "." + name.substring(name.lastIndexOf(".") + 1);
                } else {
                    destFilename = name;
                    destFileSuffix = ".bin";
                }
            }
            dest = new File(System.getProperty("java.io.tmpdir"), destFilename + System.currentTimeMillis() + destFileSuffix);
            dest.deleteOnExit();
        } catch (Exception e) {
            log.error(message.getString("fs.download.fail.io"), e);
            return null;
        }
        return downloadFile(path, filename, dest);
    }

    @Override
    public File downloadFile(String path, String filename, File destFile) {
        if (minioClient == null) {
            log.error(message.getString("fs.minio.not.connected"));
            return null;
        }
        String[] extract = extractBucketAndObjectNameFromPathAndFilename(path, filename);
        String bucketName = extract[0];
        String objectName = extract[1];
        try {
            minioClient.downloadObject(DownloadObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .filename(destFile.getCanonicalPath())
                    .extraHeaders(this.property.getApiHeaders())
                    .build());
            return destFile;
        } catch (Exception e) {
            log.error(message.getString("fs.minio.download.error")
                    .replace("${bucketName}", String.valueOf(bucketName))
                    .replace("${objectName}", String.valueOf(objectName)), e);
            return null;
        }
    }

    @Override
    public ByteArrayOutputStream downloadStream(String path, String filename) {
        if (minioClient == null) {
            log.error(message.getString("fs.minio.not.connected"));
            return null;
        }
        String[] extract = extractBucketAndObjectNameFromPathAndFilename(path, filename);
        String bucketName = extract[0];
        String objectName = extract[1];
        try (InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .extraHeaders(this.property.getApiHeaders())
                .build())) {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            byte[] cache = new byte[4096];
            int len;
            while ((len = stream.read(cache)) != -1) {
                outStream.write(cache, 0, len);
            }
            return outStream;
        } catch (Exception e) {
            log.error(message.getString("fs.minio.download.error")
                    .replace("${bucketName}", String.valueOf(bucketName))
                    .replace("${objectName}", String.valueOf(objectName)), e);
            return null;
        }
    }

    @Override
    public boolean deleteFile(String path, String filename) {
        if (minioClient == null) {
            log.error(message.getString("fs.minio.not.connected"));
            return false;
        }
        String[] extract = extractBucketAndObjectNameFromPathAndFilename(path, filename);
        String bucketName = extract[0];
        String objectName = extract[1];
        if (filename.endsWith("/")) {
            if (this.property.isCanDeleteFolderRecursive()) {
                return deleteFileRecursive(bucketName, objectName);
            } else {
                log.error(message.getString("fs.minio.delete.reject")
                        .replace("${alias}", this.property.getAlias()));
                return false;
            }
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .extraHeaders(this.property.getApiHeaders())
                    .build());
            log.debug(message.getString("fs.minio.delete.success")
                    .replace("${bucketName}", String.valueOf(bucketName))
                    .replace("${objectName}", String.valueOf(objectName)));
            return true;
        } catch (Exception e) {
            log.error(message.getString("fs.minio.delete.error")
                    .replace("${bucketName}", String.valueOf(bucketName))
                    .replace("${objectName}", String.valueOf(objectName)), e);
            return false;
        }
    }

    private boolean deleteFileRecursive(String bucketName, String objectName) {
        try {
            boolean isDir = false;
            String objectNameAsPath = objectName.endsWith("/") ? objectName : (objectName + "/");
            Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .prefix(objectNameAsPath)
                    .extraHeaders(this.property.getApiHeaders())
                    .build());
            for (Result<Item> listObject : results) {
                isDir = true;
                if (!objectNameAsPath.equals(listObject.get().objectName())) {
                    deleteFileRecursive(bucketName, listObject.get().objectName());
                }
            }
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(isDir ? (objectName + "/") : objectName)
                    .extraHeaders(this.property.getApiHeaders())
                    .build());
            log.debug(message.getString("fs.minio.delete.success")
                    .replace("${bucketName}", String.valueOf(bucketName))
                    .replace("${objectName}", String.valueOf(objectName)));
            return true;
        } catch (Exception e) {
            log.error(message.getString("fs.minio.delete.error")
                    .replace("${bucketName}", String.valueOf(bucketName))
                    .replace("${objectName}", String.valueOf(objectName)), e);
            return false;
        }
    }

    /**
     * 删除指定版本的文件
     * @param path 绝对路径
     * @param filename 文件名
     * @return 成功与否
     */
    public boolean deleteFileVersioned(String path, String filename, String version) {
        if (minioClient == null) {
            log.error(message.getString("fs.minio.not.connected"));
            return false;
        }
        String[] extract = extractBucketAndObjectNameFromPathAndFilename(path, filename);
        String bucketName = extract[0];
        String objectName = extract[1];
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .versionId(version)
                    .extraHeaders(this.property.getApiHeaders())
                    .build());
            log.debug(message.getString("fs.minio.delete.with.version.success")
                    .replace("${bucketName}", String.valueOf(bucketName))
                    .replace("${objectName}", String.valueOf(objectName))
                    .replace("${version}", String.valueOf(version)));
            return true;
        } catch (Exception e) {
            log.error(message.getString("fs.minio.delete.with.version.error")
                    .replace("${bucketName}", String.valueOf(bucketName))
                    .replace("${objectName}", String.valueOf(objectName))
                    .replace("${version}", String.valueOf(version)), e);
            return false;
        }
    }

    /**
     * 获取临时文件访问地址
     *
     * @param path     桶名称/文件路径
     * @param filename 文件名称
     * @param duration 超时时间
     * @return 访问地址
     */
    public String getTemporaryObjectUrl(String path, String filename, Duration duration) {
        return getTemporaryObjectUrl(path, filename, null, duration);
    }

    /**
     * 获取临时文件访问地址
     *
     * @param path     桶名称/文件路径
     * @param filename 文件名称
     * @param version  文件版本
     * @param duration 超时时间
     * @return 访问地址
     */
    public String getTemporaryObjectUrl(String path, String filename, String version, Duration duration) {
        if (minioClient == null) {
            log.error(message.getString("fs.minio.not.connected"));
            return null;
        }
        String[] extract = extractBucketAndObjectNameFromPathAndFilename(path, filename);
        String bucketName = extract[0];
        String objectName = extract[1];
        try {
            GetPresignedObjectUrlArgs.Builder builder = GetPresignedObjectUrlArgs.builder()
                    .extraHeaders(this.property.getApiHeaders())
                    .bucket(bucketName)
                    .object(objectName)
                    // same as library code
                    .expiry((int) TimeUnit.SECONDS.toSeconds(duration.getSeconds()), TimeUnit.SECONDS);
            if (version != null) {
                builder.versionId(version);
            }
            return minioClient.getPresignedObjectUrl(builder.build());
        } catch (Exception e) {
            if (version == null) {
                log.error(message.getString("fs.minio.get.url.error")
                        .replace("${bucketName}", String.valueOf(bucketName))
                        .replace("${objectName}", String.valueOf(objectName)), e);
            } else {
                log.error(message.getString("fs.minio.get.url.with.version.error")
                        .replace("${bucketName}", String.valueOf(bucketName))
                        .replace("${objectName}", String.valueOf(objectName))
                        .replace("${version}", String.valueOf(version)), e);
            }
            return null;
        }
    }

}
