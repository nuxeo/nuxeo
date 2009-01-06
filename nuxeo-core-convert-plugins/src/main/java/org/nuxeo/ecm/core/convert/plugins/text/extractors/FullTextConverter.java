package org.nuxeo.ecm.core.convert.plugins.text.extractors;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.runtime.api.Framework;

public class FullTextConverter implements Converter {

    private static final Log log = LogFactory.getLog(FullTextConverter.class);

    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {

        String srcMT=null;
        try {
            srcMT = blobHolder.getBlob().getMimeType();
        } catch (ClientException e) {
            throw new ConversionException("Unable to get source MimeType",e);
        }
        ConversionService cs = Framework.getLocalService(ConversionService.class);

        String converterName = cs.getConverterName(srcMT, "text/plain");

        if (converterName!=null) {
            return cs.convert(converterName, blobHolder, parameters);
        }
        else {
            log.debug("Unable to find full text extractor for source mime type" + srcMT);
            return new SimpleBlobHolder(new StringBlob(""));
        }
    }

    public void init(ConverterDescriptor descriptor) {
    }

}
