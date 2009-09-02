package org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces;

import org.nuxeo.ecm.platform.publisher.api.PublicationNode;

/**
 * Interface for {@link PublicationNode} marshaler
 * 
 * @author tiry
 * 
 */
public interface PublicationNodeMarshaler {

    String marshalPublicationNode(PublicationNode node)
            throws PublishingMarshalingException;;

    PublicationNode unMarshalPublicationNode(String data)
            throws PublishingMarshalingException;;
}
