package moe.icyr.test;

import com.github.tobato.fastdfs.domain.upload.ThumbImage;
import moe.icyr.spring.starter.filesystem.api.FileSystem;
import moe.icyr.spring.starter.filesystem.api.entity.FileInfo;
import moe.icyr.spring.starter.filesystem.api.entity.FileSystemProperty;
import moe.icyr.spring.starter.filesystem.fdfs.FdfsFileSystem;
import moe.icyr.spring.starter.filesystem.fdfs.entity.FdfsProperty;
import moe.icyr.spring.starter.filesystem.sftp.SftpSshjFileSystem;
import moe.icyr.spring.starter.filesystem.sftp.entity.SftpProperty;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.Collection;

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
        SftpSshjFileSystem fs = new SftpSshjFileSystem(sftpProperty);
        f = fs.createDirectory(path);
        System.out.println(MessageFormat.format("mkdir: {0}", f));
        f = fs.upload(Base64.getEncoder().encodeToString(context.getBytes(StandardCharsets.UTF_8)), path, filename);
        System.out.println(MessageFormat.format("upload: {0}", f));
        Collection<FileInfo> infos = fs.list("/home/icelitty/test", true, false);
        System.out.println(MessageFormat.format("list: {0}", infos));
        String base64 = fs.downloadBase64(path, filename);
        System.out.println(MessageFormat.format("download: {0}", base64 == null ? "-1" : base64.length()));
        String value = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
        System.out.println(MessageFormat.format("context: {0}", value));
        f = fs.deleteFile(path, filename);
        System.out.println(MessageFormat.format("delete: {0}", f));
        fs.close();
    }

    public static void main(String[] args) {
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
        FileSystem fs = new FdfsFileSystem(fdfsProperty);
        f = fs.upload(context.getBytes(StandardCharsets.UTF_8), pathBuffer, filenameBuffer);
        System.out.println(MessageFormat.format("upload: {0} path: {1} filename: {2}", f, pathBuffer, filenameBuffer));
        byte[] bytes = fs.downloadBytes(pathBuffer.toString().concat("/").concat(filenameBuffer.toString()), null);
        System.out.println(MessageFormat.format("download: {0}", bytes == null ? "-1" : bytes.length));
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
        FileInfo fileInfo1 = fs.peekFile(pathBuffer1.toString(), filenameBuffer1.toString());
        System.out.println(MessageFormat.format("info: {0}", fileInfo1));
        f = fs.appenderUpload(context2, pathBuffer1, filenameBuffer1, context2.length, fileInfo1.getSize());
        System.out.println(MessageFormat.format("second upload: {0}", f));
        FileInfo fileInfo2 = fs.peekFile(pathBuffer1.toString(), filenameBuffer1.toString());
        System.out.println(MessageFormat.format("info: {0}", fileInfo2));
        f = fs.appenderUpload(context2, pathBuffer1, filenameBuffer1, context2.length, fileInfo2.getSize());
        System.out.println(MessageFormat.format("third upload: {0}", f));
        byte[] bytes1 = fs.downloadBytes(pathBuffer1.toString(), filenameBuffer1.toString());
        System.out.println(MessageFormat.format("download: {0}", bytes1 == null ? "-1" : bytes1.length));
        String value1 = new String(bytes1, StandardCharsets.UTF_8);
        System.out.println(MessageFormat.format("context: {0}", value1));
        f = fs.deleteFile(null, pathBuffer1.toString().concat("/").concat(filenameBuffer1.toString()));
        System.out.println(MessageFormat.format("delete: {0}", f));
        fs.close();
    }

}
