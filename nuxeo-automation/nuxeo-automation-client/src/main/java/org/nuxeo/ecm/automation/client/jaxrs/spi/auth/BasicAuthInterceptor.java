package org.nuxeo.ecm.automation.client.jaxrs.spi.auth;

import org.nuxeo.ecm.automation.client.jaxrs.RequestInterceptor;
import org.nuxeo.ecm.automation.client.jaxrs.spi.Connector;
import org.nuxeo.ecm.automation.client.jaxrs.spi.Request;
import org.nuxeo.ecm.automation.client.jaxrs.util.Base64;

public class BasicAuthInterceptor implements RequestInterceptor {

    protected String token;

    public BasicAuthInterceptor(String username, String password) {
        setAuth(username, password);
    }

    public void setAuth(String username, String password) {
        String info = username.concat(":").concat(password);
        token = "Basic " + Base64.encode(info);
    }

    @Override
    public void processRequest(Request request, Connector connector) {
        request.put("Authorization", token);
    }

}
