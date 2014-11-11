/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.automation.client;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.HttpContext;
import org.mockito.Mockito;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpConnector;
import org.nuxeo.ecm.automation.client.jaxrs.spi.AbstractAutomationClient;
import org.nuxeo.ecm.automation.client.jaxrs.spi.Connector;
import org.nuxeo.ecm.automation.client.model.OperationDocumentation;
import org.nuxeo.ecm.automation.client.model.OperationRegistry;

/**
 * @author dmetzler
 *
 */
public class MockedHttpAutomationClient extends AbstractAutomationClient {

    private HttpClient http;

    public MockedHttpAutomationClient(String url) {
        super(url);
        init();
    }

    private void init() {

        registry = mock(OperationRegistry.class);
        http = mock(HttpClient.class);


    }

    public void setResponse(String contentType, String responseBody) {

        http = mock(HttpClient.class);

        try {
            Mockito.when(
                    http.execute(any(HttpUriRequest.class),
                            any(HttpContext.class))).thenReturn(
                    prepareResponse(200, responseBody,contentType));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private HttpResponse prepareResponse(int expectedResponseStatus,
            String expectedResponseBody, String contentType) {
        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(
                new ProtocolVersion("HTTP", 1, 1), expectedResponseStatus, ""));
        response.setStatusCode(expectedResponseStatus);
        try {
            StringEntity entity = new StringEntity(expectedResponseBody);
            entity.setContentType(contentType);
            response.setEntity(entity);

        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
        return response;
    }

    @Override
    protected Session login(Connector connector) {
        return createSession(connector, new LoginInfo("Administrator"));
    }

    @Override
    public synchronized void shutdown() {
        http = null;
    }

    class MockedOperationDocumentation extends OperationDocumentation{

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public MockedOperationDocumentation (String operationId) {
            super(operationId);
        }

        /**
         * @param string
         */
        public void withParams(String... paramIds) {
            for (String paramId : paramIds) {
                Param p = new Param();
                p.name = paramId;
                params.add(p);
            }
        }
    }

    /**
     * @param string
     * @return
     */
    public MockedOperationDocumentation addOperation(String opeationId) {
        MockedOperationDocumentation opDoc = new MockedOperationDocumentation(opeationId);
        when(registry.getOperation(opeationId)).thenReturn(opDoc);
        return opDoc;
    }

    @Override
    protected Connector newConnector() {
        return new HttpConnector(http, 0);
    }
}
