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

package org.nuxeo.ecm.webengine.gwt.client.ui;

import org.nuxeo.ecm.webengine.gwt.client.Framework;
import org.nuxeo.ecm.webengine.gwt.client.UI;
import org.nuxeo.ecm.webengine.gwt.client.http.HttpRequest;
import org.nuxeo.ecm.webengine.gwt.client.http.HttpResponse;

import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.user.client.ui.Widget;

/**
 * A view is an item that may switch in a busy state.
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class View extends Item {

    public View(String name) {
        super(name);
    }
    
    public View(String name, Widget widget) {
        super(name, widget); 
    }
   
    public void showBusy() {
        UI.showBusy();
    }

    public void hideBusy() {
        UI.hideBusy();
    }

    public ViewRequest get(String uri) {
        return new ViewRequest(this, RequestBuilder.GET, uri);
    }
    
    public ViewRequest post(String uri) {
        return new ViewRequest(this, RequestBuilder.POST, uri);        
    }
    
    public void onRequestSuccess(HttpRequest request, HttpResponse response) {
        hideBusy();
        onRequestCompleted(request, response);
    }
    
    /**
     * Override this when needed
     * @param response
     */
    public void onRequestCompleted(HttpRequest request, HttpResponse response) {
        // do nothing
    }
    
    public void onRequestFailure(HttpRequest request, Throwable cause) {
        hideBusy();
        Framework.handleError(cause);
    }
    
}
