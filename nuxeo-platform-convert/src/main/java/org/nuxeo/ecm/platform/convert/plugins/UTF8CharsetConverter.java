package org.nuxeo.ecm.platform.convert.plugins;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.InputStreamBlob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;

import com.ibm.icu.text.CharsetDetector;

public class UTF8CharsetConverter implements Converter {

    @Override
    public void init(ConverterDescriptor descriptor) {
        ;
    }

    @Override
    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {
        Blob originalBlob;
        String path;
        try {
            originalBlob = blobHolder.getBlob();
            path = blobHolder.getFilePath();
        } catch (ClientException e) {
            throw new ConversionException("Cannot fetch content of blob", e);
        }
        Blob transcodedBlob;
        try {
            transcodedBlob = convert(originalBlob);
        } catch (IOException e) {
            throw new ConversionException("Cannot transcode " + path + " to UTF-8", e);
        }
        return new SimpleBlobHolder(transcodedBlob);
    }

    protected Blob convert(Blob blob) throws IOException {
        String mimetype = blob.getMimeType();
        if (mimetype == null || !mimetype.startsWith("text/")) {
            return blob;
        }
        String encoding = blob.getEncoding();
        if (encoding != null && "UTF-8".equals(encoding)) {
            return blob;
        }
        InputStream in = new BufferedInputStream(blob.getStream());
        String filename = blob.getFilename();
        if (encoding == null || encoding.length() == 0) {
            encoding = detectEncoding(in);
        }
        if ("UTF-8".equals(encoding)) {
            blob =  new InputStreamBlob(in);
        } else {
            String content = IOUtils.toString(in,
                encoding);
            blob =  new StringBlob(content);
        }
        blob.setMimeType(mimetype);
        blob.setEncoding("UTF-8");
        blob.setFilename(filename);
        return blob;
    }

    protected String detectEncoding(InputStream in) throws IOException {
        CharsetDetector detector = new CharsetDetector();
        detector.setText(in);
        return detector.detect().getName();
    }

}
