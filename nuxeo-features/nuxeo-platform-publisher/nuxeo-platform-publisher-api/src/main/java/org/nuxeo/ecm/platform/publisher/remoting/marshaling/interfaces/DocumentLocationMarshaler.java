package org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces;

import org.nuxeo.ecm.core.api.DocumentLocation;

/**
 * Interface for {@link DocumentLocation} marshaler
 * 
 * @author tiry
 * 
 */
public interface DocumentLocationMarshaler {

    String marshalDocumentLocation(DocumentLocation docLoc)
            throws PublishingMarshalingException;;

    DocumentLocation unMarshalDocumentLocation(String data)
            throws PublishingMarshalingException;;

    void setOriginatingServer(String serverName);
}
