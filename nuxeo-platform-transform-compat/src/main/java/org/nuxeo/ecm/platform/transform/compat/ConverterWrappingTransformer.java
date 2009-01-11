package org.nuxeo.ecm.platform.transform.compat;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.platform.transform.interfaces.Transformer;

public class ConverterWrappingTransformer implements Converter {

    protected Transformer transformer;

    public ConverterWrappingTransformer(Transformer transformer) {
        this.transformer = transformer;
    }

    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {
        // TODO Auto-generated method stub
        return null;
    }

    public void init(ConverterDescriptor descriptor) {
        // TODO Auto-generated method stub

    }

}
