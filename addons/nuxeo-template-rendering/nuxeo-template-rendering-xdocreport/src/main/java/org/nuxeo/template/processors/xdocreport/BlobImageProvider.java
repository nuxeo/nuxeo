package org.nuxeo.template.processors.xdocreport;

import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.ecm.core.api.Blob;

import fr.opensagres.xdocreport.core.document.ImageFormat;
import fr.opensagres.xdocreport.document.images.AbstractInputStreamImageProvider;

/**
 * XDocReport wrapper for a Picture stored in a Nuxeo Blob
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * 
 */
public class BlobImageProvider extends AbstractInputStreamImageProvider {

    protected final Blob blob;

    protected final ImageFormat imageFormat;

    public BlobImageProvider(Blob blob) {
        super(false);
        this.blob = blob;
        this.imageFormat = ImageFormat.getFormatByResourceName(blob.getFilename());
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        return blob.getStream();
    }

    @Override
    public ImageFormat getImageFormat() {
        return imageFormat;
    }

}
