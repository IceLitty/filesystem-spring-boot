package moe.icyr.spring.starter.filesystem.sftp.entity;

import net.schmizz.sshj.xfer.InMemoryDestFile;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * @author IceLitty
 * @since 1.0
 */
public class InMemoryFile extends InMemoryDestFile {

    private final ByteArrayOutputStream stream;

    public InMemoryFile(ByteArrayOutputStream stream) {
        this.stream = stream;
    }

    @Override
    public OutputStream getOutputStream() {
        return stream;
    }

}
