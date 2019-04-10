package org.nuxeo.ecm.platform.importer.random;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;

public class PartialTextExtractor implements Converter {

    public static final double TEXT_RATIO = 0.01;

    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {
        try {
            Blob blob = blobHolder.getBlob();

            String data = blob.getString();
            int endIdx = new Double(data.length() * TEXT_RATIO).intValue();
            String txtData = data.substring(0, endIdx);
            return new SimpleBlobHolder(new StringBlob(txtData));

        } catch (Exception e) {
            throw new ConversionException(
                    "error extracting partial text content", e);
        }
    }

    public void init(ConverterDescriptor descriptor) {
    }

}
