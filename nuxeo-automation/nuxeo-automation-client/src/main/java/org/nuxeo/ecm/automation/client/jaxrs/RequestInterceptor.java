package org.nuxeo.ecm.automation.client.jaxrs;

import org.nuxeo.ecm.automation.client.jaxrs.spi.Connector;
import org.nuxeo.ecm.automation.client.jaxrs.spi.Request;


public interface RequestInterceptor {

    void processRequest(Request request, Connector connector);

}
