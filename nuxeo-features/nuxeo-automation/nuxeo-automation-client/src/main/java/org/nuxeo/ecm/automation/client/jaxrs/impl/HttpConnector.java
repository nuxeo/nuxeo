/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 *     ataillefer
 */
package org.nuxeo.ecm.automation.client.jaxrs.impl;

import java.nio.charset.UnsupportedCharsetException;
import java.util.Map;

import javax.mail.internet.MimeMultipart;

import java.nio.charset.StandardCharsets;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import org.nuxeo.ecm.automation.client.RemoteException;
import org.nuxeo.ecm.automation.client.jaxrs.spi.Connector;
import org.nuxeo.ecm.automation.client.jaxrs.spi.Request;

/**
 * Connector wrapping a {@link HttpClient} instance.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class HttpConnector implements Connector {

    protected final HttpClient http;

    /**
     * Timeout in milliseconds for the socket, connection manager and connection used by {@link #http}.
     */
    protected final int httpConnectionTimeout;

    protected final HttpContext ctx;

    protected String basicAuth;

    public HttpConnector(HttpClient http) {
        this(http, 0);
    }

    /**
     * Allows to set a timeout for the HTTP connection to avoid infinite or quite long waiting periods if:
     * <ul>
     * <li>Nuxeo is broken or running into an infinite loop</li>
     * <li>the network doesn't respond at all</li>
     * </ul>
     *
     * @since 5.7
     */
    public HttpConnector(HttpClient http, int httpConnectionTimeout) {
        this(http, new BasicHttpContext(), httpConnectionTimeout);
    }

    public HttpConnector(HttpClient http, HttpContext ctx) {
        this(http, ctx, 0);
    }

    /**
     * @see #HttpConnector(HttpClient, long)
     * @since 5.7
     */
    public HttpConnector(HttpClient http, HttpContext ctx, int httpConnectionTimeout) {
        ctx.setAttribute(HttpClientContext.COOKIE_STORE, new BasicCookieStore());
        this.http = http;
        this.httpConnectionTimeout = httpConnectionTimeout;
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
                        entity = new StringEntity(obj.toString(), StandardCharsets.UTF_8);
                    } catch (UnsupportedCharsetException e) {
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

    protected Object execute(Request request, HttpUriRequest httpReq) throws Exception {
        for (Map.Entry<String, String> entry : request.entrySet()) {
            httpReq.setHeader(entry.getKey(), entry.getValue());
        }
        HttpResponse resp = executeRequestWithTimeout(httpReq);
        HttpEntity entity = resp.getEntity();
        try {
            int status = resp.getStatusLine().getStatusCode();
            if (entity == null) {
                if (status < 400) {
                    return null;
                }
                throw new RemoteException(status, "ServerError", "Server Error", "");
            }
            Header ctypeHeader = entity.getContentType();
            if (ctypeHeader == null) { // handle broken responses with no ctype
                if (status != 200) {
                    // this may happen when login failed
                    throw new RemoteException(status, "ServerError", "Server Error", "");
                }
                return null; // cannot handle responses with no ctype
            }
            String ctype = ctypeHeader.getValue();
            String disp = null;
            Header[] hdisp = resp.getHeaders("Content-Disposition");
            if (hdisp != null && hdisp.length > 0) {
                disp = hdisp[0].getValue();
            }
            return request.handleResult(status, ctype, disp, entity.getContent());
        } finally {
            // needed to properly release resources and return the connection to the pool
            EntityUtils.consume(entity);
        }
    }

    protected HttpResponse executeRequestWithTimeout(HttpUriRequest httpReq) throws Exception {

        // Set timeout for the socket, connection manager
        // and connection itself
        if (httpConnectionTimeout > 0) {
            HttpParams httpParams = http.getParams();
            httpParams.setIntParameter("http.socket.timeout", httpConnectionTimeout);
            httpParams.setIntParameter("http.connection-manager.timeout", httpConnectionTimeout);
            httpParams.setIntParameter("http.connection.timeout", httpConnectionTimeout);
        }
        return http.execute(httpReq, ctx);
    }

}
