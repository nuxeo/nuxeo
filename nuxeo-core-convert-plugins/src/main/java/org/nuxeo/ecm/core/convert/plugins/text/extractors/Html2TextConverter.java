package org.nuxeo.ecm.core.convert.plugins.text.extractors;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;

public class Html2TextConverter implements Converter {

    private static final Log log = LogFactory.getLog(Html2TextConverter.class);

    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {


          InputStream stream = null;
          try {
              stream = blobHolder.getBlob().getStream();
              HtmlHandler html2text = new HtmlHandler();
              String text = html2text.parse(stream);

              return new SimpleCachableBlobHolder(new StringBlob(text,
                      "text/plain"));
          } catch (Exception e) {
              throw new ConversionException("Error during Html2Text conversion", e);
          } finally {
              if (stream != null) {
                  try {
                      stream.close();
                  } catch (IOException e) {
                      log.error("Error while closing Blob stream", e);
                  }
              }
          }
    }

    public void init(ConverterDescriptor descriptor) {
    }

}
