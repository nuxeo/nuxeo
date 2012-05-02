package org.nuxeo.template.jaxrs;

import org.nuxeo.ecm.core.api.Blob;

import fr.opensagres.xdocreport.remoting.resources.domain.BinaryData;

/**
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * 
 */
public class BinaryDataWrapper {

    public static BinaryData wrap(Blob blob) throws Exception {
        // fall back to dumb byte[] constructor because of change 93648fa3e82b
        // in XDocReport API
        return new BinaryData(blob.getByteArray(), blob.getFilename(),
                blob.getMimeType());
    }

}
