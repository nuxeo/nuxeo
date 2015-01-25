package org.nuxeo.ecm.platform.convert.tests;

import java.io.IOException;
import java.io.InputStream;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.platform.convert.plugins.UTF8CharsetConverter;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class CanTranscodeCharsetsTest extends NXRuntimeTestCase {

    @Test
    public void transcodeLatin1() throws IOException, ClientException {
        Blob blob;
        try (InputStream in = CanTranscodeCharsetsTest.class.getResource("/latin1.txt").openStream()) {
            Blob latin1Blob = Blobs.createBlob(in, "text/plain");
            BlobHolder holder = new SimpleBlobHolder(latin1Blob);
            UTF8CharsetConverter encoder = new UTF8CharsetConverter();
            blob = encoder.convert(holder, null).getBlob();
        }
        Assert.assertThat(blob.getMimeType(), Matchers.is("text/plain"));
        Assert.assertThat(blob.getEncoding(), Matchers.is("UTF-8"));
        String content = blob.getString();
        Assert.assertThat(content, Matchers.is("Test de prévisualisation avant rattachement"));
    }
}
