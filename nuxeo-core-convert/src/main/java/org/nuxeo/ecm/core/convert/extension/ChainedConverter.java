package org.nuxeo.ecm.core.convert.extension;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.service.ConversionServiceImpl;
import org.nuxeo.ecm.core.convert.service.MimeTypeTranslationHelper;

public class ChainedConverter implements Converter {

    protected List<String> steps = new ArrayList<String>();

    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {

        try {
            String srcMT = blobHolder.getBlob().getMimeType();

            BlobHolder result = blobHolder;
            for (String dstMT : steps) {
                String converterName = MimeTypeTranslationHelper.getConverterName(srcMT, dstMT);
                if (converterName==null) {
                    throw new ConversionException("Chained conversion error : unable to find converter between " + srcMT + " and " + dstMT);
                }

                Converter converter = ConversionServiceImpl.getConverter(converterName);

                result = converter.convert(result, parameters);
                srcMT = dstMT;
            }

            return result;
        }
        catch (ClientException e) {
            throw new ConversionException("error while trying to determine converter name", e);
        }
    }

    public void init(ConverterDescriptor descriptor) {
        steps.addAll(descriptor.getSteps());
        steps.add(descriptor.getDestinationMimeType());
    }

    public List<String> getSteps() {
        return steps;
    }
}
