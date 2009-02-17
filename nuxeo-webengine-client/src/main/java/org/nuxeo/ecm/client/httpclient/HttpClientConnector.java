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
package org.nuxeo.ecm.client.httpclient;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.nuxeo.ecm.client.Connection;
import org.nuxeo.ecm.client.Connector;
import org.nuxeo.ecm.client.ContentHandlerRegistry;

/**
 * @author matic
 *
 */
public class HttpClientConnector implements Connector {

    protected final String baseURL;
    protected final ContentHan
    
    public HttpClientConnector(String baseURL, ContentHandlerRegistry contentHandlerRegistry) {
        this.baseURL = baseURL;
        this.contentHandlerRegistry = contentHandlerRegistry;
    }

    protected HttpClient support = new HttpClient();
    
    public Connection delete(String url) {
        DeleteMethod method = new DeleteMethod(url);
        support.executeMethod(method);
        return new HttpClientConnection(method);
        try {
            support.executeMethod(method);
        } catch (HttpException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.client.Connector#delete(java.lang.String, java.util.Map)
     */
    public Connection delete(String url, Map<String, Object> params) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.client.Connector#get(java.lang.String)
     */
    public Connection get(String url) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.client.Connector#get(java.lang.String, java.util.Map)
     */
    public Connection get(String url, Map<String, Object> params) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.client.Connector#getBaseURL()
     */
    public URL getBaseURL() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.client.Connector#head(java.lang.String)
     */
    public Connection head(String url) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.client.Connector#head(java.lang.String, java.util.Map)
     */
    public Connection head(String url, Map<String, Object> params) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.client.Connector#post(java.lang.String, java.lang.Object)
     */
    public Connection post(String url, Object content) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.client.Connector#post(java.lang.String, java.util.Map)
     */
    public Connection post(String url, Map<String, Object> params) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.client.Connector#post(java.lang.String, java.lang.Object, java.util.Map)
     */
    public Connection post(String url, Object content,
            Map<String, Object> params) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.client.Connector#put(java.lang.String, java.lang.Object)
     */
    public Connection put(String url, Object content) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.client.Connector#put(java.lang.String, java.util.Map)
     */
    public Connection put(String url, Map<String, Object> params) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.client.Connector#put(java.lang.String, java.lang.Object, java.util.Map)
     */
    public Connection put(String url, Object content, Map<String, Object> params) {
        // TODO Auto-generated method stub
        return null;
    }

}
