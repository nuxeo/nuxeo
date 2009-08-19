package org.nuxeo.ecm.platform.publisher.remoting.server;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.RemotePublisherMarshaler;

/**
 * Generic invoker interface
 * 
 * @author tiry
 * 
 */
public interface PublicationInvokationHandler {

    void init(RemotePublisherMarshaler marshaler);

    String invoke(String methodDate, String params) throws ClientException;

}
