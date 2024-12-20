package moe.icyr.test;

import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import moe.icyr.spring.starter.filesystem.api.FileSystem;
import moe.icyr.spring.starter.filesystem.api.entity.FileInfo;
import moe.icyr.spring.starter.filesystem.factory.FileSystemFactory;
import moe.icyr.spring.starter.filesystem.entity.FileSystemProfile;
import moe.icyr.spring.starter.filesystem.minio.MinIOFileSystem;
import moe.icyr.spring.starter.filesystem.minio.entity.UploadReqParams;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"LoggingSimilarMessage", "DataFlowIssue"})
@Slf4j
@SpringBootTest
class TestApplicationTests {

    @Autowired
    private FileSystemProfile profile;

    @Test
    void testProfile() {
        log.info(String.valueOf(profile));
        //noinspection EmptyTryBlock
        try (FileSystem<?, ?> fs = FileSystemFactory.make(profile.getProfile("sftp1"));) {
        }
    }

    @Test
    void testFtp() {
        String path = "/测试目录";
        String filename = "测试文件.txt";
        String context = "文本";
        boolean f;
        try (FileSystem<?, ?> fs = FileSystemFactory.make(profile.getProfile("ftp1"));) {
            f = fs.createDirectory(path);
            log.info("mkdir: {}", f);
            f = fs.upload(Base64.getEncoder().encodeToString(context.getBytes(StandardCharsets.UTF_8)), path, filename);
            log.info("upload: {}", f);
            List<? extends FileInfo<?>> infos = fs.list("/", true, false, -1);
            log.info("list: {}", infos);
            String base64 = fs.downloadBase64(path, filename);
            log.info("download: {}", base64 == null ? "-1" : base64.length());
            String value = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
            log.info("context: {}", value);
            f = fs.deleteFile(path, filename);
            log.info("delete: {}", f);
        }
    }

    // SFTP client need jvm arguments on linux server: -Djava.security.egd=file:/dev/./urandom

    @Test
    void testSftp() {
        String path = "/home/icelitty/test/a";
        String filename = "测试文件.txt";
        String context = "文本";
        boolean f;
        try (FileSystem<?, ?> fs = FileSystemFactory.make(profile.getProfile("sftp1"));) {
            f = fs.createDirectory(path);
            log.info("mkdir: {}", f);
            f = fs.upload(Base64.getEncoder().encodeToString(context.getBytes(StandardCharsets.UTF_8)), path, filename);
            log.info("upload: {}", f);
            List<? extends FileInfo<?>> infos = fs.list("/home/icelitty/test", true, false, -1);
            log.info("list: {}", infos);
            String base64 = fs.downloadBase64(path, filename);
            log.info("download: {}", base64 == null ? "-1" : base64.length());
            String value = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
            log.info("context: {}", value);
            f = fs.deleteFile(path, filename);
            log.info("delete: {}", f);
        }
    }

    @Test
    void testFdfs() {
        StringBuffer pathBuffer = new StringBuffer();
        StringBuffer filenameBuffer = new StringBuffer("测试文件.txt");
        String context = "文本";
        boolean f;
        try (FileSystem<?, ?> fs = FileSystemFactory.make(profile.getProfile("fdfs1"));) {
            f = fs.upload(context.getBytes(StandardCharsets.UTF_8), pathBuffer, filenameBuffer);
            log.info("upload: {} path: {} filename: {}", f, pathBuffer, filenameBuffer);
            byte[] bytes = fs.downloadBytes(pathBuffer.toString().concat("/").concat(filenameBuffer.toString()), null);
            log.info("download: {}", bytes == null ? "-1" : bytes.length);
            String value = new String(bytes, StandardCharsets.UTF_8);
            log.info("context: {}", value);
            f = fs.deleteFile(null, pathBuffer.toString().concat("/").concat(filenameBuffer.toString()));
            log.info("delete: {}", f);
            log.info("--------------------------------");
            // -----------------------------------------
            StringBuffer pathBuffer1 = new StringBuffer("group1");
            StringBuffer filenameBuffer1 = new StringBuffer(".txt");
            byte[] context1 = "文".getBytes(StandardCharsets.UTF_8);
            byte[] context2 = "本文".getBytes(StandardCharsets.UTF_8);
            f = fs.appenderUpload(context1, pathBuffer1, filenameBuffer1, context1.length, 0);
            log.info("first upload: {} path: {} filename: {}", f, pathBuffer1, filenameBuffer1);
            FileInfo<?> fileInfo1 = fs.peekFile(pathBuffer1.toString(), filenameBuffer1.toString());
            log.info("info: {}", fileInfo1);
            f = fs.appenderUpload(context2, pathBuffer1, filenameBuffer1, context2.length, fileInfo1.getSize());
            log.info("second upload: {}", f);
            FileInfo<?> fileInfo2 = fs.peekFile(pathBuffer1.toString(), filenameBuffer1.toString());
            log.info("info: {}", fileInfo2);
            f = fs.appenderUpload(context2, pathBuffer1, filenameBuffer1, context2.length, fileInfo2.getSize());
            log.info("third upload: {}", f);
            byte[] bytes1 = fs.downloadBytes(pathBuffer1.toString(), filenameBuffer1.toString());
            log.info("download: {}", bytes1 == null ? "-1" : bytes1.length);
            String value1 = new String(bytes1, StandardCharsets.UTF_8);
            log.info("context: {}", value1);
            f = fs.deleteFile(null, pathBuffer1.toString().concat("/").concat(filenameBuffer1.toString()));
            log.info("delete: {}", f);
        }
    }

    @Test
    void testMinio() {
        String bucketName = "test-buck-123";
        String objectPath = "testFolder";
        String path = bucketName + "/" + objectPath;
        String filename = "测试文件.txt";
        String content = "文本";
        boolean f;
        try (FileSystem<?, Item> fs = FileSystemFactory.make(profile.getProfile("MinIOServ1"));) {
            f = fs.createDirectory(bucketName);
            log.info("create bucket: {}", f);
            f = fs.createDirectory(path);
            log.info("mkdir: {}", f);
            f = fs.upload(new File("C:\\Users\\IceRain\\Downloads\\mask.png"), path, "测试图片.png");
            log.info("upload from file: {} path: {} filename: {}", f, path, filename);
            f = fs.upload(content.getBytes(StandardCharsets.UTF_8), path, filename);
            log.info("upload from memory: {} path: {} filename: {}", f, path, filename);
            StringBuilder txt = new StringBuilder();
            for (int i = 0; i < 5000000; i++) {
                txt.append(content);
            }
            byte[] b2 = txt.toString().getBytes(StandardCharsets.UTF_8);
            f = ((MinIOFileSystem) fs).uploadWithMetadata(b2, path, "2" + filename,
                    UploadReqParams.builder().partSize(5*1024*1024).metaData(Collections.singletonMap("custom-meta-xxx", "xxx-123-456")).build());
            log.info("upload from memory with multi parts (all size: {}MB) and meta data: {} path: {} filename: {}", b2.length/1024/1024, f, path, filename);
            List<? extends FileInfo<?>> infos = fs.list(null);
            log.info("list buckets: {}", infos);
            infos = fs.list(path, true, false, -1);
            log.info("list objects in bucket or specific folder: {}", infos);
            byte[] bytes1 = fs.downloadBytes(path, filename);
            log.info("download: {}", bytes1 == null ? "-1" : bytes1.length);
            String value = new String(bytes1, StandardCharsets.UTF_8);
            log.info("context: {}", value);
            File file = fs.downloadFile(path, filename);
            log.info("download as file or tempFile: {} {}", file.length(), file.getAbsolutePath());
            List<FileInfo<Item>> infos2 = ((MinIOFileSystem) fs).list(path, true, false, -1, true);
            infos2.stream().filter(FileInfo::isFile).filter(fi -> ("2" + filename).equals(fi.getFilename())).filter(fi -> fi.getOriginalInfo().versionId() != null && !"null".equals(fi.getOriginalInfo().versionId())).findAny().ifPresent(fi -> {
                boolean _f = ((MinIOFileSystem) fs).deleteFileVersioned(fi.getAbsolutePath(), fi.getFilename(), fi.getOriginalInfo().versionId());
                log.info("delete file versioned: {} {} {} {}", _f, fi.getAbsolutePath(), fi.getFilename(), fi.getOriginalInfo().versionId());
            });
            f = fs.deleteFile(path, filename);
            log.info("delete file: {}", f);
            f = fs.deleteFile(bucketName, objectPath + "/");
            log.info("delete folder: {}", f);
        }
    }

}
