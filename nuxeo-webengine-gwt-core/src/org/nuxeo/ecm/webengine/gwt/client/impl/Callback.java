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

import org.nuxeo.ecm.webengine.gwt.client.Framework;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Callback implements RequestCallback {

    
    public void onError(Request request, Throwable exception) {
        // Couldn't connect to server (could be timeout, SOP violation, etc.)
        System.out.println("ERROR");
        System.out.println("######################@@@@@@@@");
    }

    public void onResponseReceived(Request request, Response response) {
//        GWT.log("############ GET RESP: "+response.getHeadersAsString(), null);
//        GWT.log("############ GET RESP: "+response.getStatusCode(), null);
//        GWT.log("############ GET RESP: "+response.getStatusText(), null);
//        GWT.log("############ GET RESP: "+response.getText(), null);
        
        if (200 == response.getStatusCode()) {
            //GWT.log("############ GET RESP: 200", null);
            //result = new String(response.getText());
            Framework.getContext().setInputObject(response.getText());            
        } else {
            // Handle the error.  Can get the status text from response.getStatusText()
        }
    }       
    

}
