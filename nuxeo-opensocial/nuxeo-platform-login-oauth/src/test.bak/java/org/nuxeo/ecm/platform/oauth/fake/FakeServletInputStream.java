package org.nuxeo.ecm.platform.oauth.fake;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletInputStream;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;

public class FakeServletInputStream extends ServletInputStream {

    private Blob blob;

    private InputStream stream;

    public FakeServletInputStream(String data) throws IOException {
        blob = new StringBlob(data);
        stream = blob.getStream();
    }

    public FakeServletInputStream(InputStream in) {
        stream = in;
    }

    public FakeServletInputStream(Blob blob) throws IOException {
        this.blob = blob;
        stream = blob.getStream();
    }

    @Override
    public int read() throws IOException {
        return stream.read();
    }

}
