package org.nuxeo.ecm.platform.publisher.remoting.invoker;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.RemotePublisherMarshaler;

import java.util.List;

/**
 * 
 * Interface for remote invocation of publishing
 * 
 * @author tiry
 * 
 */
public interface RemotePublicationInvoker {

    void init(String baseURL, String userName, String password,
            RemotePublisherMarshaler marshaler);

    Object invoke(String methodName, List<Object> params)
            throws ClientException;

}
