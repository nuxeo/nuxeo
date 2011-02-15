package org.nuxeo.opensocial.container.server.webcontent.api;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

/**
 * @author Stéphane Fourrier
 */
public interface WebContentAdapter<T extends WebContentData> {
    public void feedFrom(T data) throws ClientException;

    public T getData() throws ClientException;;
}
