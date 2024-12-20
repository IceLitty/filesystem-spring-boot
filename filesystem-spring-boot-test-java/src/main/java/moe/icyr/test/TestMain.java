package moe.icyr.test;

import com.github.tobato.fastdfs.domain.upload.ThumbImage;
import io.minio.messages.Item;
import moe.icyr.spring.starter.filesystem.api.entity.FileInfo;
import moe.icyr.spring.starter.filesystem.api.entity.FileSystemProperty;
import moe.icyr.spring.starter.filesystem.fdfs.FdfsFileSystem;
import moe.icyr.spring.starter.filesystem.fdfs.entity.FdfsProperty;
import moe.icyr.spring.starter.filesystem.minio.MinIOFileSystem;
import moe.icyr.spring.starter.filesystem.minio.entity.MinIOProperty;
import moe.icyr.spring.starter.filesystem.minio.entity.UploadReqParams;
import moe.icyr.spring.starter.filesystem.sftp.SftpSshjFileSystem;
import moe.icyr.spring.starter.filesystem.sftp.entity.SftpProperty;
import net.schmizz.sshj.sftp.RemoteResourceInfo;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author IceLitty
 * @since 1.0
 */
public class TestMain {

    public static void mainSshj(String[] args) {
        String path = "/home/icelitty/test/a";
        String filename = "测试文件.txt";
        String context = "文本";
        boolean f;
        FileSystemProperty property = new FileSystemProperty();
        property.setType("sftp");
        property.setIp("127.0.0.1");
        property.setPort(22);
        property.setUsername("icelitty");
        property.setPassword("icelitty");
        SftpProperty sftpProperty = new SftpProperty(property);
        sftpProperty.setPrivateKey("C:\\Users\\IceRain\\.ssh\\wsl\\icelitty\\id_rsa_enc_icelitty");
        try (SftpSshjFileSystem fs = new SftpSshjFileSystem(sftpProperty);) {
            f = fs.createDirectory(path);
            System.out.println(MessageFormat.format("mkdir: {0}", f));
            f = fs.upload(Base64.getEncoder().encodeToString(context.getBytes(StandardCharsets.UTF_8)), path, filename);
            System.out.println(MessageFormat.format("upload: {0}", f));
            Collection<FileInfo<RemoteResourceInfo>> infos = fs.list("/home/icelitty/test", true, false, -1);
            System.out.println(MessageFormat.format("list: {0}", infos));
            String base64 = fs.downloadBase64(path, filename);
            System.out.println(MessageFormat.format("download: {0}", base64 == null ? "-1" : base64.length()));
            String value = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
            System.out.println(MessageFormat.format("context: {0}", value));
            f = fs.deleteFile(path, filename);
            System.out.println(MessageFormat.format("delete: {0}", f));
        }
    }

    public static void mainFdfs(String[] args) {
        StringBuffer pathBuffer = new StringBuffer();
        StringBuffer filenameBuffer = new StringBuffer("测试文件.txt");
        String context = "文本";
        boolean f;
        FileSystemProperty property = new FileSystemProperty();
        property.setType("fdfs");
        property.setIp("192.168.8.103");
        property.setPort(22122);
        property.setUsername("-");
        property.setPassword("-");
        FdfsProperty fdfsProperty = new FdfsProperty(property);
        fdfsProperty.setSoTimeout(3000);
        fdfsProperty.setConnectTimeout(10000);
        fdfsProperty.setCharset(StandardCharsets.UTF_8);
        fdfsProperty.setThumbImage(new ThumbImage(100, 100));
        try (FdfsFileSystem fs = new FdfsFileSystem(fdfsProperty);) {
            f = fs.upload(context.getBytes(StandardCharsets.UTF_8), pathBuffer, filenameBuffer);
            System.out.println(MessageFormat.format("upload: {0} path: {1} filename: {2}", f, pathBuffer, filenameBuffer));
            byte[] bytes = fs.downloadBytes(pathBuffer.toString().concat("/").concat(filenameBuffer.toString()), null);
            System.out.println(MessageFormat.format("download: {0}", bytes == null ? "-1" : bytes.length));
            @SuppressWarnings("DataFlowIssue")
            String value = new String(bytes, StandardCharsets.UTF_8);
            System.out.println(MessageFormat.format("context: {0}", value));
            f = fs.deleteFile(null, pathBuffer.toString().concat("/").concat(filenameBuffer.toString()));
            System.out.println(MessageFormat.format("delete: {0}", f));
            System.out.println("--------------------------------");
            // -----------------------------------------
            StringBuffer pathBuffer1 = new StringBuffer("group1");
            StringBuffer filenameBuffer1 = new StringBuffer(".txt");
            byte[] context1 = "文".getBytes(StandardCharsets.UTF_8);
            byte[] context2 = "本文".getBytes(StandardCharsets.UTF_8);
            f = fs.appenderUpload(context1, pathBuffer1, filenameBuffer1, context1.length, 0);
            System.out.println(MessageFormat.format("first upload: {0} path: {1} filename: {2}", f, pathBuffer1, filenameBuffer1));
            FileInfo<?> fileInfo1 = fs.peekFile(pathBuffer1.toString(), filenameBuffer1.toString());
            System.out.println(MessageFormat.format("info: {0}", fileInfo1));
            f = fs.appenderUpload(context2, pathBuffer1, filenameBuffer1, context2.length, fileInfo1.getSize());
            System.out.println(MessageFormat.format("second upload: {0}", f));
            FileInfo<?> fileInfo2 = fs.peekFile(pathBuffer1.toString(), filenameBuffer1.toString());
            System.out.println(MessageFormat.format("info: {0}", fileInfo2));
            f = fs.appenderUpload(context2, pathBuffer1, filenameBuffer1, context2.length, fileInfo2.getSize());
            System.out.println(MessageFormat.format("third upload: {0}", f));
            byte[] bytes1 = fs.downloadBytes(pathBuffer1.toString(), filenameBuffer1.toString());
            System.out.println(MessageFormat.format("download: {0}", bytes1 == null ? "-1" : bytes1.length));
            @SuppressWarnings("DataFlowIssue")
            String value1 = new String(bytes1, StandardCharsets.UTF_8);
            System.out.println(MessageFormat.format("context: {0}", value1));
            f = fs.deleteFile(null, pathBuffer1.toString().concat("/").concat(filenameBuffer1.toString()));
            System.out.println(MessageFormat.format("delete: {0}", f));
        }
    }

    public static void mainMinIO(String[] args) {
        String bucketName = "test-buck-123";
        String objectPath = "testFolder";
        String path = bucketName + "/" + objectPath;
        String filename = "测试文件.txt";
        String content = "文本";
        boolean f;
        FileSystemProperty property = new FileSystemProperty();
        property.setType("minio");
        property.setIp("127.0.0.1");
        property.setPort(9000);
        property.setUsername("U2ALk6yzh2qsPeZWC9Ar");
        property.setPassword("ecbGiQE2SGZaYk52teqPVA08mOlmAvFQKEn7HM2n");
        MinIOProperty minioProperty = new MinIOProperty(property);
        try (MinIOFileSystem fs = new MinIOFileSystem(minioProperty);) {
            f = fs.createDirectory(bucketName);
            System.out.println("create bucket: " + f);
            f = fs.createDirectory(path);
            System.out.println("mkdir: " + f);
            f = fs.upload(new File("C:\\Users\\IceRain\\Downloads\\mask.png"), path, "测试图片.png");
            System.out.println("upload from file: " + f + " path: " + path + " filename: " + filename);
            f = fs.upload(content.getBytes(StandardCharsets.UTF_8), path, filename);
            System.out.println("upload from memory: " + f + " path: " + path + " filename: " + filename);
            StringBuilder txt = new StringBuilder();
            for (int i = 0; i < 5000000; i++) {
                txt.append(content);
            }
            byte[] b2 = txt.toString().getBytes(StandardCharsets.UTF_8);
            f = fs.uploadWithMetadata(b2, path, "2" + filename,
                    UploadReqParams.builder().partSize(5*1024*1024).metaData(Collections.singletonMap("custom-meta-xxx", "xxx-123-456")).build());
            System.out.println("upload from memory with multi parts (all size: " + (b2.length/1024/1024) + "MB) and meta data: " + f + " path: " + path + " filename: " + filename);
            List<? extends FileInfo<?>> infos = fs.list(null);
            System.out.println("list buckets: " + infos);
            infos = fs.list(path, true, false, -1);
            System.out.println("list objects in bucket or specific folder: " + infos);
            byte[] bytes1 = fs.downloadBytes(path, filename);
            System.out.println("download: " + (bytes1 == null ? "-1" : bytes1.length));
            String value = new String(bytes1, StandardCharsets.UTF_8);
            System.out.println("context: " + value);
            File file = fs.downloadFile(path, filename);
            System.out.println("download as file or tempFile: " + file.length() + " " + file.getAbsolutePath());
            List<FileInfo<Item>> infos2 = fs.list(path, true, false, -1, true);
            infos2.stream().filter(FileInfo::isFile).filter(fi -> ("2" + filename).equals(fi.getFilename())).filter(fi -> fi.getOriginalInfo().versionId() != null && !"null".equals(fi.getOriginalInfo().versionId())).findAny().ifPresent(fi -> {
                boolean _f = fs.deleteFileVersioned(fi.getAbsolutePath(), fi.getFilename(), fi.getOriginalInfo().versionId());
                System.out.println("delete file versioned: " + _f + " " + fi.getAbsolutePath() + " " + fi.getFilename() + " " + fi.getOriginalInfo().versionId());
            });
            f = fs.deleteFile(path, filename);
            System.out.println("delete file: " + f);
            f = fs.deleteFile(bucketName, objectPath + "/");
            System.out.println("delete folder: " + f);
        }
    }

}
