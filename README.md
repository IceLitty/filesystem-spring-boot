# 使用
- FTP / SFTP 等标准文件服务
（SFTP可能需要添加JVM参数
`-Djava.security.egd=file:/dev/./urandom`
以使SecurityRandom在linux服务器环境不受阻断）
```java
// 使用Springboot自动装载配置文件（使用本项目的starter）
// 不使用starter的方案参考 filesystem-spring-boot-test-java 模块内的样例
@Autowired
private FileSystemProfile profile;
@Test
void testFtp() {
    String path = "/测试目录";
    String filename = "测试文件.txt";
    String context = "文本";
    boolean f;
    // 生成客户端实例（泛型可根据实际类型写明 <客户端类型, 文件类型>，用于直接操作依赖库连接对象或查看文件原始对象信息）
    FileSystem<?, ?> fs = FileSystemFactory.make(profile.getProfile("ftp1"));
    // 创建目录
    f = fs.createDirectory(path);
    log.info("mkdir: {}", f);
    // 上传文件
    f = fs.upload(Base64.getEncoder().encodeToString(context.getBytes(StandardCharsets.UTF_8)), path, filename);
    log.info("upload: {}", f);
    // 列出路径下的文件
    Collection<FileInfo<?>> infos = fs.list("/", true, false);
    log.info("list: {}", infos);
    // 下载文件
    String base64 = fs.downloadBase64(path, filename);
    log.info("download: {}", base64 == null ? "-1" : base64.length());
    String value = base64 == null ? "" : new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
    log.info("context: {}", value);
    // 删除文件
    f = fs.deleteFile(path, filename);
    log.info("delete: {}", f);
    // 关闭客户端（亦可使用try-with-resource）
    fs.close();
}
```

- FastDFS
```java
// 使用Springboot自动装载配置文件（使用本项目的starter）
@Autowired
private FileSystemProfile profile;
@Test
void testFdfs() {
    StringBuffer pathBuffer = new StringBuffer();
    StringBuffer filenameBuffer = new StringBuffer("测试文件.txt");
    String context = "文本";
    boolean f;
    // 创建实例
    FileSystem fs = FileSystemFactory.make(profile.getProfile("fdfs1"));
    // 上传完整文件
    f = fs.upload(context.getBytes(StandardCharsets.UTF_8), pathBuffer, filenameBuffer);
    log.info("upload: {} path: {} filename: {}", f, pathBuffer, filenameBuffer);
    // 下载文件
    byte[] bytes = fs.downloadBytes(pathBuffer.toString().concat("/").concat(filenameBuffer.toString()), null);
    log.info("download: {}", bytes == null ? "-1" : bytes.length);
    String value = bytes == null ? "" : new String(bytes, StandardCharsets.UTF_8);
    log.info("context: {}", value);
    // 删除文件
    f = fs.deleteFile(null, pathBuffer.toString().concat("/").concat(filenameBuffer.toString()));
    log.info("delete: {}", f);
    log.info("--------------------------------");
    // 测试2：断点续传环境
    StringBuffer pathBuffer1 = new StringBuffer("group1");
    StringBuffer filenameBuffer1 = new StringBuffer(".txt");
    byte[] context1 = "文".getBytes(StandardCharsets.UTF_8);
    byte[] context2 = "本文".getBytes(StandardCharsets.UTF_8);
    // 上传首个Chunk
    f = fs.appenderUpload(context1, pathBuffer1, filenameBuffer1, context1.length, 0);
    log.info("first upload: {} path: {} filename: {}", f, pathBuffer1, filenameBuffer1);
    // 查看文件信息
    FileInfo fileInfo1 = fs.peekFile(pathBuffer1.toString(), filenameBuffer1.toString());
    log.info("info: {}", fileInfo1);
    // 上传第二个Chunk
    f = fs.appenderUpload(context2, pathBuffer1, filenameBuffer1, context2.length, fileInfo1.getSize());
    log.info("second upload: {}", f);
    // 第二次查看文件信息
    FileInfo fileInfo2 = fs.peekFile(pathBuffer1.toString(), filenameBuffer1.toString());
    log.info("info: {}", fileInfo2);
    // 上传第三个Chunk（和第二个一样）
    f = fs.appenderUpload(context2, pathBuffer1, filenameBuffer1, context2.length, fileInfo2.getSize());
    log.info("third upload: {}", f);
    // 下载文件
    byte[] bytes1 = fs.downloadBytes(pathBuffer1.toString(), filenameBuffer1.toString());
    log.info("download: {}", bytes1 == null ? "-1" : bytes1.length);
    String value1 = bytes1 == null ? "" : new String(bytes1, StandardCharsets.UTF_8);
    log.info("context: {}", value1);
    // 删除文件
    f = fs.deleteFile(null, pathBuffer1.toString().concat("/").concat(filenameBuffer1.toString()));
    log.info("delete: {}", f);
    // 关闭客户端
    fs.close();
}
```

- MinIO
```java
// 使用Springboot自动装载配置文件（使用本项目的starter）
@Autowired
private FileSystemProfile profile;
@Test
void testMinIO() {
    // 样例桶名称
    String bucketName = "test-buck-123";
    // 样例对象路径（前缀）
    String objectPath = "testFolder";
    // 样例对象名称（后缀）
    String filename = "测试文件.txt";
    // 样例对象内容
    String content = "文本";
    // API路径使用可任意拼接，只需保证路径以桶名称开头，文件名以对象名称结尾即可。按MinIO规范则区分为桶名称及对象完整路径，按文件系统规范则区分为文件所在路径及文件名称。
    String path = bucketName + "/" + objectPath;
    boolean f;
    // 创建实例
    try (FileSystem<?, Item> fs = FileSystemFactory.make(profile.getProfile("MinIOServ1"));) {
        // 创建桶（路径名不包含/）
        f = fs.createDirectory(bucketName);
        log.info("create bucket: {}", f);
        // 创建路径（路径名包含/）（建议直接使用上传方法使MinIO自动创建路径）
        f = fs.createDirectory(path);
        log.info("mkdir: {}", f);
        // 上传本地文件
        f = fs.upload(new File("C:\\Users\\xxx\\Downloads\\mask.png"), path, "测试图片.png");
        log.info("upload from file: {} path: {} filename: {}", f, path, filename);
        // 上传流
        f = fs.upload(content.getBytes(StandardCharsets.UTF_8), path, filename);
        log.info("upload from memory: {} path: {} filename: {}", f, path, filename);
        // 分段上传
        StringBuilder txt = new StringBuilder();
        for (int i = 0; i < 5000000; i++) {
            txt.append(content);
        }
        byte[] b2 = txt.toString().getBytes(StandardCharsets.UTF_8);
        f = ((MinIOFileSystem) fs).uploadWithMetadata(b2, path, "2" + filename,
                UploadReqParams.builder().partSize(5*1024*1024).metaData(Collections.singletonMap("custom-meta-xxx", "xxx-123-456")).build());
        log.info("upload from memory with multi parts (all size: {}MB) and meta data: {} path: {} filename: {}", b2.length/1024/1024, f, path, filename);
        // 列出桶
        List<? extends FileInfo<?>> infos = fs.list(null);
        log.info("list buckets: {}", infos);
        // 列出桶内指定路径下的文件
        infos = fs.list(path, true, false, -1);
        log.info("list objects in bucket or specific folder: {}", infos);
        // 下载字节
        byte[] bytes1 = fs.downloadBytes(path, filename);
        log.info("download: {}", bytes1 == null ? "-1" : bytes1.length);
        String value = new String(bytes1, StandardCharsets.UTF_8);
        log.info("context: {}", value);
        // 直接下载到文件/临时文件（文件不可预先创建，临时文件会挂载JVM退出时删除）
        File file = fs.downloadFile(path, filename);
        log.info("download as file or tempFile: {} {}", file.length(), file.getAbsolutePath());
        // 查出带版本号的文件并删除
        List<FileInfo<Item>> infos2 = ((MinIOFileSystem) fs).list(path, true, false, -1, true);
        infos2.stream().filter(FileInfo::isFile).filter(fi -> ("2" + filename).equals(fi.getFilename())).filter(fi -> fi.getOriginalInfo().versionId() != null && !"null".equals(fi.getOriginalInfo().versionId())).findAny().ifPresent(fi -> {
            boolean _f = ((MinIOFileSystem) fs).deleteFileVersioned(fi.getAbsolutePath(), fi.getFilename(), fi.getOriginalInfo().versionId());
            log.info("delete file versioned: {} {} {} {}", _f, fi.getAbsolutePath(), fi.getFilename(), fi.getOriginalInfo().versionId());
        });
        // 删除文件
        f = fs.deleteFile(path, filename);
        log.info("delete file: {}", f);
        // 递归删除目录及目录下所有文件（需以/结尾）
        f = fs.deleteFile(bucketName, objectPath + "/");
        log.info("delete folder: {}", f);
    }
}
```

# 依赖配置
- 使用Springboot
```xml
<dependency>
    <groupId>moe.icyr</groupId>
    <artifactId>filesystem-spring-boot-starter</artifactId>
    <version>1.3</version>
</dependency>
```
- 不使用Springboot
```xml
<dependency>
    <groupId>moe.icyr</groupId>
    <artifactId>filesystem-spring-boot-api</artifactId>
    <version>1.3</version>
</dependency>
<dependency>
    <groupId>moe.icyr</groupId>
    <artifactId>filesystem-spring-boot-ftp</artifactId>
    <version>1.3</version>
</dependency>
<dependency>
    <groupId>moe.icyr</groupId>
    <artifactId>filesystem-spring-boot-sftp</artifactId>
    <version>1.3</version>
</dependency>
<!-- 警告：该fdfs使用的连接器客户端依赖了Springboot子模块，不建议非Springboot情况下使用该实现 -->
<dependency>
    <groupId>moe.icyr</groupId>
    <artifactId>filesystem-spring-boot-fast-dfs</artifactId>
    <version>1.3</version>
</dependency>
```

# 约定配置
或不依赖Springboot自行构建`moe.icyr.spring.starter.filesystem.api.entity.FileSystemProperty`
配置文件对象用于对应客户端的构造方法`moe.icyr.spring.starter.filesystem.api.FileSystem`。
```yaml
file-system:
  # 手动指定某种type对应的FileSystem实现
  factory:
    ftp: moe.icyr.spring.starter.filesystem.ftp.FtpFileSystem
#    sftp: moe.icyr.spring.starter.filesystem.sftp.SftpFileSystem
    sftp: moe.icyr.spring.starter.filesystem.sftp.SftpSshjFileSystem
    minio: moe.icyr.spring.starter.filesystem.minio.MinIOFileSystem
  profiles:
    - # 类型
      type: ftp
      # 别名
      alias: 'ftp1'
      # IP地址
      ip: '127.0.0.1'
      # 端口号
      port: 21
      # 用户名，FTP匿名登录使用anonymous，密码任意字符串
      username: 'icelitty'
      # 密码
      password: '123'
      # 不同类型端的额外参数
      external:
        # FTP字符集
        charset: 'gbk'
        # FTP断线重连尝试次数
        retries: 5
    - type: sftp
      alias: 'sftp1'
      ip: '127.0.0.1'
      port: 22
      username: 'icelitty'
      password: 'icelitty'
      external:
        # SSH私钥登录方式，配合上述password可实现加密私钥读取
        private-key: 'C:\Users\IceRain\.ssh\wsl\icelitty\id_rsa_enc_icelitty'
        # SSHJ提供的保持连接进程 发送心跳包频率
        keep-alive-second: 3
    - type: fdfs
      alias: 'fdfs1'
      ip: '192.168.8.103'
      port: 22122
      # FDFS没有auth模式，但两个登录参数不能为空，随便填
      username: '-'
      password: '-'
      external:
        # 支持tobato FDFS客户端的以下参数传递
        soTimeout: 3000
        connectTimeout: 10000
        charset: UTF-8
        thumbImage:
          width: 100
          height: 100
        # 附加别的tracker服务器地址
#        trackerList:
#          - 'otherIp:port'
#        pool:
#          max-total:
#          test-while-idle:
#          block-when-exhausted:
#          max-wait-millis:
#          max-total-per-key:
#          max-idle-per-key:
#          min-idle-per-key:
#          min-evictable-idle-time-millis:
#          time-between-eviction-runs-millis:
#          num-tests-per-eviction-run:
#          jmx-name-base:
#          jmx-name-prefix:
#          soft-min-evictable-idle-time-millis:
#          test-on-create:
#          test-on-borrow:
    - type: minio
      alias: 'MinIOServ1'
      # 可输入部分或完整URL如 https://localhost:9000 http://[::]:9000 127.0.0.1:9000 127.0.0.1
      ip: '127.0.0.1'
      port: 9000
      # 推荐使用官方推荐的Token模式登录
      username: 'U2ALk6yzh2qsPeZWC9Ar'
      password: 'ecbGiQE2SGZaYk52teqPVA08mOlmAvFQKEn7HM2n'
      external:
        # 允许上传时自动创建桶，默认false
        canCreateBucket: true
        # 允许删除时自动递归删除路径下的子文件，默认false
        canDeleteFolderRecursive: true
        # 上传流时分割单次请求大小，默认-1不进行分割，单位字节，根据MinIO接口规范，需要大于5MB且小于5GB
        uploadDefaultPartSize: -1
        # 是否自定义HttpClient，默认false，通过调用 connectWithCustomHttpClient() 方法传入OkHttpClient
        customHttpClient: false
        # 与MinIO API 交互时的自定义Header
        headers:
          Custom-Header-Key: customValue
        # 上传文件时的固定自定义Metadata，也可使用 uploadWithMetadata() 方法传入动态的Metadata
        userMetadata:
          custom-meta-key: customMetaValue
```
