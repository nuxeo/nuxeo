package org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Interface for {@link DocumentModel} marshaler
 * 
 * @author tiry
 * 
 */
public interface DocumentModelMarshaler {

    String marshalDocument(DocumentModel doc)
            throws PublishingMarshalingException;;

    DocumentModel unMarshalDocument(String data, CoreSession coreSession)
            throws PublishingMarshalingException;;

    void setOriginatingServer(String serverName);
}
