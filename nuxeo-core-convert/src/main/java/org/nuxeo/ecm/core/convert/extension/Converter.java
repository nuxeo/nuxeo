package org.nuxeo.ecm.core.convert.extension;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;

public interface Converter {

    void init(ConverterDescriptor descriptor);

    BlobHolder convert(BlobHolder blobHolder,Map<String, Serializable> parameters) throws ConversionException;
}
