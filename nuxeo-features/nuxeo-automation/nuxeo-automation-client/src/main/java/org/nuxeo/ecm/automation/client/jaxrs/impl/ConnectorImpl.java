/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.jaxrs.impl;

import java.util.Map;

import javax.mail.internet.MimeMultipart;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.nuxeo.ecm.automation.client.jaxrs.RemoteException;
import org.nuxeo.ecm.automation.client.jaxrs.spi.Connector;
import org.nuxeo.ecm.automation.client.jaxrs.spi.Request;

/**
 * Connector wrapping a {@link HttpClient} instance.
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ConnectorImpl implements Connector {

    protected final HttpClient http;

    protected final HttpContext ctx;

    protected String basicAuth;

    public ConnectorImpl(HttpClient http) {
        this(http, new BasicHttpContext());
    }

    public ConnectorImpl(HttpClient http, HttpContext ctx) {
        ctx.setAttribute(ClientContext.COOKIE_STORE, new BasicCookieStore());
        this.http = http;
        this.ctx = ctx;
    }

    public void setBasicAuth(String auth) {
        this.basicAuth = auth;
    }

    public String getBasicAuth() {
        return basicAuth;
    }

    public Object execute(Request request) throws Exception {
        if (request.getMethod() == Request.POST) {
            HttpPost post = new HttpPost(request.getUrl());
            Object obj = request.getEntity();
            if (obj != null) {
                HttpEntity entity = null;
                if (request.isMultiPart()) {
                    entity = new MultipartRequestEntity((MimeMultipart) obj);
                } else {
                    entity = new StringEntity(obj.toString(), "UTF-8");
                }
                post.setEntity(entity);
            }
            return execute(request, post);
        } else {
            HttpGet get = new HttpGet(request.getUrl());
            return execute(request, get);
        }
    }

    protected Object execute(Request request, HttpUriRequest httpReq)
            throws Exception {
        for (Map.Entry<String, String> entry : request.entrySet()) {
            httpReq.setHeader(entry.getKey(), entry.getValue());
        }
        HttpResponse resp = http.execute(httpReq, ctx);
        HttpEntity entity = resp.getEntity();
        int status = resp.getStatusLine().getStatusCode();
        if (entity == null) {
            if (status < 400) {
                return null;
            }
            throw new RemoteException(status, "ServerError", "Server Error",
                    null);
        }
        Header ctypeHeader = entity.getContentType();
        if (ctypeHeader == null) { // handle broken responses with no ctype
            if (status != 200) {
                // this may happen when login failed
                throw new RemoteException(status, "ServerError",
                        "Server Error", null);
            }
            return null; // cannot handle responses with no ctype
        }
        String ctype = ctypeHeader.getValue().toLowerCase();
        String disp = null;
        Header[] hdisp = resp.getHeaders("Content-Disposition");
        if (hdisp != null && hdisp.length > 0) {
            disp = hdisp[0].getValue();
        }
        return request.handleResult(status, ctype, disp, entity.getContent());
    }

}
