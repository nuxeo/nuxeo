package org.nuxeo.ecm.core.convert.tests;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.core.convert.extension.ExternalConverter;

public class NotAvailableConverter implements ExternalConverter {

    public ConverterCheckResult isConverterAvailable() {
        return new ConverterCheckResult("Please install someting", "Can not find external converter");
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
