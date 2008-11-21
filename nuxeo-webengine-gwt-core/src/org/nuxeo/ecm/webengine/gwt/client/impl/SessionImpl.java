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

import org.nuxeo.ecm.webengine.gwt.client.Session;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SessionImpl implements Session {

    
    public Object get(String url) {
        try {
            RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
            Request request = builder.sendRequest(null, new RequestCallback() {
                public void onError(Request request, Throwable exception) {
                   // Couldn't connect to server (could be timeout, SOP violation, etc.)
                    System.out.println("ERROR");
                }

                public void onResponseReceived(Request request, Response response) {
                  if (200 == response.getStatusCode()) {
                      // Process the response in response.getText()
                  } else {
                    // Handle the error.  Can get the status text from response.getStatusText()
                  }
                }       
              });
        } catch (Exception e) {
            e.printStackTrace();
            GWT.log("Failed to start in debug mode", e);
        }
        return null;
    }

    public Object post(String url) {
        // TODO Auto-generated method stub
        return null;
    }
    
    public Object put(String url) {
        // TODO Auto-generated method stub
        return null;
    }
    
    public Object delete(String url) {
        // TODO Auto-generated method stub
        return null;
    }
    
    public Object head(String url) {
        // TODO Auto-generated method stub
        return null;
    }
    
    public boolean login(String username, String password) {
        return true;
    }

    public boolean logout() {
        return true;
    }

}
