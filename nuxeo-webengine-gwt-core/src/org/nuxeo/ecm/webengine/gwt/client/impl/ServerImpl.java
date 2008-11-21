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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.gwt.client.impl;

import org.nuxeo.ecm.webengine.gwt.client.Server;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.URL;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ServerImpl implements Server {
    

    protected String basePath; // base path used to prefix paths
    
    public ServerImpl() {
        this (null);
    }
    
    public ServerImpl(String basePath) {
        if (basePath != null && basePath.endsWith("/")) {            
            this.basePath = basePath.substring(0, basePath.length()-1); 
        } else {
            this.basePath = basePath;
        }
    }

    public String normalize(String uri) {
        if (basePath != null) {
            if (!uri.contains("://")) { // relative URL
                if (uri.startsWith("/")) {
                    uri = basePath+uri;
                } else {
                    uri = "/"+basePath+uri;  
                }
            }
        }
        return URL.encode(uri); 
    }
    
    public RequestBuilder get(String uri) {
        return new RequestBuilder(RequestBuilder.GET, normalize(uri));
    }

    public RequestBuilder post(String uri) {
        return new RequestBuilder(RequestBuilder.POST, normalize(uri));
    }
    
    public RequestBuilder put(String uri) {
        return new RequestBuilder(RequestBuilder.GET, normalize(uri));
    }
    
    public RequestBuilder delete(String uri) {
        return new RequestBuilder(RequestBuilder.GET, normalize(uri));
    }
    
    public RequestBuilder head(String uri) {
        return new RequestBuilder(RequestBuilder.GET, normalize(uri));
    }
    
    public void load(String url) {
        try {
            Callback cb = new Callback();        
            get(url).sendRequest(null, cb);
        } catch (Exception e) {
            e.printStackTrace();
            GWT.log("Failed to start in debug mode", e);
        }
    }

    
    public boolean login(String username, String password) {
        return true;
    }

    public boolean logout() {
        return true;
    }

}
