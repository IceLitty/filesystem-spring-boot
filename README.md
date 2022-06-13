# 使用
- FTP / SFTP 等标准文件服务
（SFTP可能需要添加JVM参数
`-Djava.security.egd=file:/dev/./urandom`
以使SecurityRandom在linux服务器环境不受阻断）
```java
String path = "/测试目录";
String filename = "测试文件.txt";
String context = "文本";
boolean f;
// 生成客户端实例
FileSystem fs = FileSystemFactory.make(profile.getProfile("ftp1"));
// 创建目录
f = fs.createDirectory(path);
log.info("mkdir: {}", f);
// 上传文件
f = fs.upload(Base64.getEncoder().encodeToString(context.getBytes(StandardCharsets.UTF_8)), path, filename);
log.info("upload: {}", f);
// 列出路径下的文件
Collection<FileInfo> infos = fs.list("/", true, false);
log.info("list: {}", infos);
// 下载文件
String base64 = fs.downloadBase64(path, filename);
log.info("download: {}", base64 == null ? "-1" : base64.length());
String value = base64 == null ? "" : new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
log.info("context: {}", value);
// 删除文件
f = fs.deleteFile(path, filename);
log.info("delete: {}", f);
// 关闭客户端（亦可使用try-auto-close）
fs.close();
```

- FastDFS
```java
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
```

# 依赖配置
- 使用Springboot
```xml
<dependency>
    <groupId>moe.icyr</groupId>
    <artifactId>filesystem-spring-boot-starter</artifactId>
    <version>1.0</version>
</dependency>
```
- 不使用Springboot
```xml
<dependency>
    <groupId>moe.icyr</groupId>
    <artifactId>filesystem-spring-boot-api</artifactId>
    <version>1.0</version>
</dependency>
<dependency>
    <groupId>moe.icyr</groupId>
    <artifactId>filesystem-spring-boot-ftp</artifactId>
    <version>1.0</version>
</dependency>
<dependency>
    <groupId>moe.icyr</groupId>
    <artifactId>filesystem-spring-boot-sftp</artifactId>
    <version>1.0</version>
</dependency>
<!-- 警告：该fdfs使用的连接器客户端依赖了Springboot子模块，不建议非Springboot情况下使用该实现 -->
<dependency>
    <groupId>moe.icyr</groupId>
    <artifactId>filesystem-spring-boot-fast-dfs</artifactId>
    <version>1.0</version>
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
```
