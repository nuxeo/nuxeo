package org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces;

import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;

/**
 * Interface for {@link PublishedDocument} marshaler
 * 
 * @author tiry
 * 
 */
public interface PublishedDocumentMarshaler {

    String marshalPublishedDocument(PublishedDocument pubDoc)
            throws PublishingMarshalingException;;

    PublishedDocument unMarshalPublishedDocument(String data)
            throws PublishingMarshalingException;;

}
