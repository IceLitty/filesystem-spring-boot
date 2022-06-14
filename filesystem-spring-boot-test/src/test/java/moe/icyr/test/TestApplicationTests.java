package moe.icyr.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import moe.icyr.spring.starter.filesystem.api.FileSystem;
import moe.icyr.spring.starter.filesystem.api.entity.FileInfo;
import moe.icyr.spring.starter.filesystem.factory.FileSystemFactory;
import moe.icyr.spring.starter.filesystem.entity.FileSystemProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;

@Slf4j
@SpringBootTest
class TestApplicationTests {

    @Autowired
    private FileSystemProfile profile;

    @Test
    void testProfile() {
        log.info(String.valueOf(profile));
        FileSystem fs = FileSystemFactory.make(profile.getProfile("sftp1"));
    }

    @Test
    void testFtp() {
        String path = "/测试目录";
        String filename = "测试文件.txt";
        String context = "文本";
        boolean f;
        FileSystem fs = FileSystemFactory.make(profile.getProfile("ftp1"));
        f = fs.createDirectory(path);
        log.info("mkdir: {}", f);
        f = fs.upload(Base64.getEncoder().encodeToString(context.getBytes(StandardCharsets.UTF_8)), path, filename);
        log.info("upload: {}", f);
        Collection<FileInfo> infos = fs.list("/", true, false);
        log.info("list: {}", infos);
        String base64 = fs.downloadBase64(path, filename);
        log.info("download: {}", base64 == null ? "-1" : base64.length());
        String value = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
        log.info("context: {}", value);
        f = fs.deleteFile(path, filename);
        log.info("delete: {}", f);
        fs.close();
    }

    // SFTP client need jvm arguments on linux server: -Djava.security.egd=file:/dev/./urandom

    @Test
    void testSftp() {
        String path = "/home/icelitty/test/a";
        String filename = "测试文件.txt";
        String context = "文本";
        boolean f;
        FileSystem fs = FileSystemFactory.make(profile.getProfile("sftp1"));
        f = fs.createDirectory(path);
        log.info("mkdir: {}", f);
        f = fs.upload(Base64.getEncoder().encodeToString(context.getBytes(StandardCharsets.UTF_8)), path, filename);
        log.info("upload: {}", f);
        Collection<FileInfo> infos = fs.list("/home/icelitty/test", true, false);
        log.info("list: {}", infos);
        String base64 = fs.downloadBase64(path, filename);
        log.info("download: {}", base64 == null ? "-1" : base64.length());
        String value = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
        log.info("context: {}", value);
        f = fs.deleteFile(path, filename);
        log.info("delete: {}", f);
        fs.close();
    }

    @Test
    void testFdfs() {
        StringBuffer pathBuffer = new StringBuffer();
        StringBuffer filenameBuffer = new StringBuffer("测试文件.txt");
        String context = "文本";
        boolean f;
        FileSystem fs = FileSystemFactory.make(profile.getProfile("fdfs1"));
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
        FileInfo fileInfo1 = fs.peekFile(pathBuffer1.toString(), filenameBuffer1.toString());
        log.info("info: {}", fileInfo1);
        f = fs.appenderUpload(context2, pathBuffer1, filenameBuffer1, context2.length, fileInfo1.getSize());
        log.info("second upload: {}", f);
        FileInfo fileInfo2 = fs.peekFile(pathBuffer1.toString(), filenameBuffer1.toString());
        log.info("info: {}", fileInfo2);
        f = fs.appenderUpload(context2, pathBuffer1, filenameBuffer1, context2.length, fileInfo2.getSize());
        log.info("third upload: {}", f);
        byte[] bytes1 = fs.downloadBytes(pathBuffer1.toString(), filenameBuffer1.toString());
        log.info("download: {}", bytes1 == null ? "-1" : bytes1.length);
        String value1 = new String(bytes1, StandardCharsets.UTF_8);
        log.info("context: {}", value1);
        f = fs.deleteFile(null, pathBuffer1.toString().concat("/").concat(filenameBuffer1.toString()));
        log.info("delete: {}", f);
        fs.close();
    }

}
