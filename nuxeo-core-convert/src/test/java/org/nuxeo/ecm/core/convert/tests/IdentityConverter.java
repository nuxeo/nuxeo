package org.nuxeo.ecm.core.convert.tests;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;

public class IdentityConverter implements Converter {

    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {

        Blob inputBlob;
        try {
            inputBlob = blobHolder.getBlob();
            return new SimpleCachableBlobHolder(inputBlob);
        } catch (ClientException e) {
            throw new ConversionException("Error while getting input blob", e);
        }
    }

    public void init(ConverterDescriptor descriptor) {
        // NOP
    }

}
