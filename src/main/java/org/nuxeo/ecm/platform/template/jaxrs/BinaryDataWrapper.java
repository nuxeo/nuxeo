package org.nuxeo.ecm.platform.template.jaxrs;

import org.nuxeo.ecm.core.api.Blob;

import fr.opensagres.xdocreport.remoting.resources.domain.BinaryData;

public class BinaryDataWrapper {

    public static BinaryData wrap(Blob blob) throws Exception {
        return new BinaryData(blob.getStream(), blob.getFilename(),
                blob.getMimeType());
    }

}
