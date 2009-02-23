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
 *     matic
 */
package org.nuxeo.ecm.cmis.client.app.httpclient;

import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.nuxeo.ecm.cmis.ContentManagerException;
import org.nuxeo.ecm.cmis.client.app.APPContentManager;
import org.nuxeo.ecm.cmis.client.app.Connector;
import org.nuxeo.ecm.cmis.client.app.Request;
import org.nuxeo.ecm.cmis.client.app.Response;
import org.nuxeo.ecm.cmis.client.app.SerializationManager;



/**
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a> 
 */
public class HttpClientConnector implements Connector {

    protected HttpClient client;
    protected APPContentManager cm;  

    public HttpClientConnector(APPContentManager cm) {
        this.cm = cm;
        this.client = new HttpClient();
    }
    
    public APPContentManager getAPPContentManager() {
        return cm;
    }
    
    public SerializationManager getSerializationManager() {
        return cm.getSerializationManager();
    }
    
    
    protected void setMethodParams(HttpMethod method, Request request) {
        List<String> params = request.getParameters();
        if (params != null) {
            int len=params.size()>>1;
            if (len > 0) {
                NameValuePair[] qs = new NameValuePair[len];
                for (int i=0, k=0; i<len; i++,k+=2) {
                    qs[i] =  new NameValuePair(params.get(k), params.get(k));
                }
                method.setQueryString(qs);
            }
        }        
    }

    protected void setMethodHeaders(HttpMethod method, Request request) {
        List<String> headers = request.getHeaders();
        if (headers != null) {
            int len=headers.size();
            for (int k=0; k<len; k+=2) {
                method.addRequestHeader(headers.get(k), headers.get(k));
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    protected void setMethodContent(Request request, EntityEnclosingMethod method) throws ContentManagerException {
        if (request != null) {
            Object object = request.getContent();
            method.setRequestEntity(new ObjectRequestEntity(getSerializationManager().getHandler(object.getClass()), object));
        }
    }
        
    public Response get(Request request) throws ContentManagerException {
        try {
            GetMethod method = new GetMethod(request.getUrl());
            setMethodParams(method, request);
            setMethodHeaders(method, request);
            client.executeMethod(method);
            return new HttpClientResponse(this, method);
        } catch (Exception e) {
            throw new ContentManagerException("GET request failed", e);
        }
    }
    
    public Response delete(Request request) throws ContentManagerException {
        try {
            DeleteMethod method = new DeleteMethod(request.getUrl());
            setMethodParams(method, request);
            setMethodHeaders(method, request);
            client.executeMethod(method);
            return new HttpClientResponse(this, method);
        } catch (Exception e) {
            throw new ContentManagerException("DELETE request failed", e);
        }
    }
    
    public Response head(Request request) throws ContentManagerException {
        try {
            HeadMethod method = new HeadMethod(request.getUrl());
            setMethodParams(method, request);
            setMethodHeaders(method, request);
            client.executeMethod(method);
            return new HttpClientResponse(this, method);
        } catch (Exception e) {
            throw new ContentManagerException("HEAD request failed", e);
        }
    }
    
    public Response post(Request request) throws ContentManagerException {
        try {
            PostMethod method = new PostMethod(request.getUrl());
            setMethodParams(method, request);
            setMethodHeaders(method, request);
            setMethodContent(request, method);
            client.executeMethod(method);
            return new HttpClientResponse(this, method);
        } catch (Exception e) {
            throw new ContentManagerException("POST request failed", e);
        }
    }
    
    public Response put(Request request) throws ContentManagerException {
        try {
            PutMethod method = new PutMethod(request.getUrl());
            setMethodParams(method, request);
            setMethodHeaders(method, request);
            setMethodContent(request, method);
            client.executeMethod(method);
            return new HttpClientResponse(this, method);
        } catch (Exception e) {
            throw new ContentManagerException("PUT request failed", e);
        }
    }
    
}
