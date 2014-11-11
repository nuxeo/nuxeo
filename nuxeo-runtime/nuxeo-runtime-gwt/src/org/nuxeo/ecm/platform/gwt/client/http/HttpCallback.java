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

package org.nuxeo.ecm.platform.gwt.client.http;

import org.nuxeo.ecm.platform.gwt.client.Framework;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class HttpCallback implements RequestCallback {

    protected HttpRequest request;
    
    protected void setRequest(HttpRequest request) {
        this.request = request; 
    }
    
    public HttpRequest getRequest() {
        return request;
    }
    
    public void onError(Request request, Throwable exception) {
        onFailure(exception);
    }

    public void onResponseReceived(Request request, Response response) {
        if (response.getStatusCode() < 400) {
            onSuccess(new HttpResponse(response));            
        } else {
            onFailure(new ServerException(response));
        }      
    }

    public void onFailure(Throwable cause) {
        Framework.handleError(cause);
    }
    
    public abstract void onSuccess(HttpResponse response);

}
