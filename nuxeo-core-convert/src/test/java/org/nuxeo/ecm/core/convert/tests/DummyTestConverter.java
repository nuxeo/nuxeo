package org.nuxeo.ecm.core.convert.tests;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;

public class DummyTestConverter implements Converter {

    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {
        // TODO Auto-generated method stub
        return null;
    }

    public void init(ConverterDescriptor descriptor) {



        // TODO Auto-generated method stub

    }

}
