package org.nuxeo.template.processors.xdocreport;

import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.template.processors.AbstractBindingResolver;

import fr.opensagres.xdocreport.core.document.SyntaxKind;
import fr.opensagres.xdocreport.document.images.IImageProvider;
import fr.opensagres.xdocreport.template.formatter.FieldsMetadata;
import freemarker.template.TemplateModelException;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class XDocReportBindingResolver extends AbstractBindingResolver {

    protected final FieldsMetadata metadata;

    public XDocReportBindingResolver(FieldsMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    protected String handleHtmlField(String paramName, String htmlValue) {
        metadata.addFieldAsTextStyling(paramName, SyntaxKind.Html);
        return super.handleHtmlField(paramName, htmlValue);
    }

    @Override
    protected void handleBlobField(String paramName, Blob blobValue) {
        if ("text/html".equals(blobValue.getMimeType())) {
            metadata.addFieldAsTextStyling(paramName, SyntaxKind.Html);
        }
    }

    @Override
    protected Object handlePictureField(String paramName, Blob blobValue) {
        if (blobValue == null) {
            // manage a default picture : blank one :)
            try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("blank.png")) {
                byte[] bin = FileUtils.readBytes(is);
                blobValue = new ByteArrayBlob(bin);
                blobValue.setFilename("blank.png");
                blobValue.setMimeType("image/png");
            } catch (IOException e) {
                log.error("Unable to read fake Blob", e);
            }
        }
        IImageProvider imgBlob = new BlobImageProvider(blobValue);
        metadata.addFieldAsImage(paramName);
        return imgBlob;
    }

    @Override
    protected Object handleLoop(String paramName, Object value) {
        metadata.addFieldAsList(paramName);
        try {
            return getWrapper().wrap(value);
        } catch (TemplateModelException e) {
            return null;
        }
    }

}
