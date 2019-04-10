package org.nuxeo.ecm.core.opencmis.impl.client.protocol.http;

import org.apache.commons.httpclient.HttpClient;

public interface HttpURLClientProvider {

    HttpClient getClient();

    void setClient(HttpClient client);

}