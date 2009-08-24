package org.nuxeo.ecm.platform.publisher.remoting.restHandler;

import org.nuxeo.ecm.platform.publisher.remoting.marshaling.DefaultMarshaler;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.PublishingMarshalingException;
import org.nuxeo.ecm.webengine.WebEngine;

public class RemotePubResult {

    protected Object result;

    public RemotePubResult(Object result) {
        this.result = result;
    }

    public String asXML() throws PublishingMarshalingException {
        return new DefaultMarshaler(
                WebEngine.getActiveContext().getCoreSession()).marshallResult(result);
    }

}
