package org.nuxeo.template.xdocreport.jaxrs;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

import fr.opensagres.xdocreport.remoting.resources.domain.LargeBinaryData;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class BinaryDataWrapper {

    public static LargeBinaryData wrap(Blob blob) throws Exception {

        LargeBinaryData data = new LargeBinaryData();
        data.setContent(blob.getStream());
        data.setFileName(blob.getFilename());
        data.setMimeType(blob.getMimeType());
        if (blob.getLength() > 0) {
            data.setLength(blob.getLength());
        }
        return data;
    }

    public static LargeBinaryData wrap(TemplateSourceDocument template) throws Exception {
        Blob blob = template.getTemplateBlob();
        LargeBinaryData data = wrap(blob);
        data.setResourceId(template.getAdaptedDoc().getId());
        return data;
    }

    public static LargeBinaryData wrapXml(String xml, String fileName) throws Exception {
        Blob blob = Blobs.createBlob(xml, "text/xml", null, fileName);
        return wrap(blob);
    }

}
