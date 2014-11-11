/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.jaxrs.impl;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.mail.internet.MimeMultipart;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.AbstractHttpClient;
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
public class HttpConnector implements Connector {

    protected final AbstractHttpClient http;

    protected final HttpContext ctx;

    protected String basicAuth;

    public HttpConnector(HttpClient http) {
        this(http, new BasicHttpContext());
    }

    public HttpConnector(HttpClient http, HttpContext ctx) {
        ctx.setAttribute(ClientContext.COOKIE_STORE, new BasicCookieStore());
        this.http = (AbstractHttpClient) http;
        this.ctx = ctx;
    }

    @Override
    public Object execute(Request request) {
        HttpRequestBase httpRequest = null;
        if (request.getMethod() == Request.POST) {
            HttpPost post = new HttpPost(request.getUrl());
            Object obj = request.getEntity();
            if (obj != null) {
                HttpEntity entity = null;
                if (request.isMultiPart()) {
                    entity = new MultipartRequestEntity((MimeMultipart) obj);
                } else {
                    try {
                        entity = new StringEntity(obj.toString(), "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        throw new Error("Cannot encode into UTF-8", e);
                    }
                }
                post.setEntity(entity);
            }
            httpRequest = post;
        } else {
            httpRequest = new HttpGet(request.getUrl());
        }
        try {
            return execute(request, httpRequest);
        } catch (RemoteException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Cannot execute " + request, e);
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
