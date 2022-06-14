package moe.icyr.test;

import moe.icyr.spring.starter.filesystem.api.entity.FileInfo;
import moe.icyr.spring.starter.filesystem.api.entity.FileSystemProperty;
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

    public static void main(String[] args) {
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

}
