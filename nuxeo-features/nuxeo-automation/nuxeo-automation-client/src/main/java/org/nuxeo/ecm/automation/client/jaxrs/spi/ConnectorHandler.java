package org.nuxeo.ecm.automation.client.jaxrs.spi;

import org.nuxeo.ecm.automation.client.jaxrs.RequestInterceptor;

public class ConnectorHandler implements Connector {

    protected final Connector connector;

    protected final RequestInterceptor interceptor;

    public ConnectorHandler(Connector connector, RequestInterceptor interceptor) {
       this.connector = connector;
       this.interceptor = interceptor;
    }

    @Override

    public Object execute(Request request) {
        interceptor.processRequest(request, connector);
        return connector.execute(request);
    }

}
