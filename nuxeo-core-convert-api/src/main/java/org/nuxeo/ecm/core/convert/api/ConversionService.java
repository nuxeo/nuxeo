package org.nuxeo.ecm.core.convert.api;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

public interface ConversionService {

    String getConverterName(String sourceMimeType, String destinationMimeType);

    BlobHolder convert(String converterName, BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException;

    BlobHolder convertToMimeType(String destinationMimeType, BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException;

}
