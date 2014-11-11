package org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces;

import org.nuxeo.ecm.core.api.CoreSession;

import java.util.List;
import java.util.Map;

/**
 * 
 * Interface for the Marshaler
 * 
 * @author tiry
 * 
 */
public interface RemotePublisherMarshaler {

    String marshallParameters(List<Object> params)
            throws PublishingMarshalingException;

    List<Object> unMarshallParameters(String data)
            throws PublishingMarshalingException;

    String marshallResult(Object result) throws PublishingMarshalingException;

    Object unMarshallResult(String data) throws PublishingMarshalingException;

    void setAssociatedCoreSession(CoreSession session);

    void setParameters(Map<String, String> params);

}
