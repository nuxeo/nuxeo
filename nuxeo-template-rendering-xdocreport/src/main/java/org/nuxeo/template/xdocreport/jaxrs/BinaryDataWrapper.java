package org.nuxeo.template.xdocreport.jaxrs;

import org.nuxeo.ecm.core.api.Blob;

import fr.opensagres.xdocreport.remoting.resources.domain.LargeBinaryData;

/**
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * 
 */
public class BinaryDataWrapper {

    public static LargeBinaryData wrap(Blob blob) throws Exception {

        LargeBinaryData data = new LargeBinaryData();
        data.setContent(blob.getStream());
        data.setFileName(blob.getFilename());
        data.setMimeType(blob.getMimeType());
        return data;
    }

}
