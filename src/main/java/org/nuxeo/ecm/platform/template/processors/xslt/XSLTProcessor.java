package org.nuxeo.ecm.platform.template.processors.xslt;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.platform.template.TemplateInput;
import org.nuxeo.ecm.platform.template.adapters.doc.TemplateBasedDocument;
import org.nuxeo.ecm.platform.template.processors.AbstractTemplateProcessor;
import org.nuxeo.ecm.platform.template.processors.TemplateProcessor;

public class XSLTProcessor extends AbstractTemplateProcessor implements
        TemplateProcessor {

    @Override
    public Blob renderTemplate(TemplateBasedDocument templateBasedDocument)
            throws Exception {

        BlobHolder bh = templateBasedDocument.getAdaptedDoc().getAdapter(BlobHolder.class);
        if (bh==null) {
            return null;
        }
        
        Blob xmlContent = bh.getBlob();
        if (xmlContent==null) {
            return null;
        }
                
        Blob sourceTemplateBlob = getSourceTemplate(templateBasedDocument);
                
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer(new StreamSource(
                sourceTemplateBlob.getStream()));
        transformer.setErrorListener(new ErrorListener() {
                        
            @Override
            public void warning(TransformerException exception)
                    throws TransformerException {
                log.warn("Problem during transformation", exception);
            }

            @Override
            public void fatalError(TransformerException exception)
                    throws TransformerException {
                log.error("Fatal error during transformation", exception);
            }

            @Override
            public void error(TransformerException exception)
                    throws TransformerException {
                log.error("Error during transformation", exception);
            }
        });
        transformer.setURIResolver(null);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        transformer.transform(new StreamSource(xmlContent.getStream()),
                new StreamResult(out));
                
        Blob result = new ByteArrayBlob(out.toByteArray(), "text/xml");
        String targetFileName = FileUtils.getFileNameNoExt(templateBasedDocument.getAdaptedDoc().getTitle());
        result.setFilename(targetFileName + ".xml");
        
        return result;
        
    }

    @Override
    public List<TemplateInput> getInitialParametersDefinition(Blob blob)
            throws Exception {        
        return new ArrayList<TemplateInput>();
    }

}
