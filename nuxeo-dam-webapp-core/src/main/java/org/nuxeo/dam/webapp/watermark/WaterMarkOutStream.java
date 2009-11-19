package org.nuxeo.dam.webapp.watermark;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletOutputStream;

import de.schlichtherle.io.FileOutputStream;

public class WaterMarkOutStream extends ServletOutputStream {
    protected FileOutputStream fos = null;

    public WaterMarkOutStream(File file) throws IOException {
        fos = new FileOutputStream(file, false);
    }

    @Override
    public void write(int b) throws IOException {
        fos.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        fos.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        fos.write(b, off, len);
    }

    @Override
    public void close() throws IOException {
        fos.close();
    }
}
