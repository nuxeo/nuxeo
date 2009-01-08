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

package org.nuxeo.ecm.platform.gwt.client.model;

import org.nuxeo.ecm.platform.gwt.client.Framework;
import org.nuxeo.ecm.platform.gwt.client.http.HttpResponse;
import org.nuxeo.ecm.platform.gwt.client.http.ServerException;
import org.nuxeo.ecm.platform.gwt.client.ui.HttpCommand;
import org.nuxeo.ecm.platform.gwt.client.ui.UI;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.ui.DialogBox;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class GetDocument extends HttpCommand {

    protected String id;
    
    
    public GetDocument(String id) {
        this.id = id;
    }
    
    @Override
    protected void doExecute() throws Throwable {
        get(Framework.getResourcePath("/doc?ref="+id)).send();
    }
    
    @Override
    public void onSuccess(HttpResponse response) {
         JSONValue json = response.asJSON();
         json = json.isObject().get("response").isObject().get("data");
         JSONArray ar = json.isArray();
         if (ar != null) {
             json = ar.get(0);            
         }
         Document doc = new Document(json.isObject());
         openDocument(doc);
    }
    
    protected void openDocument(Document doc) {
        UI.openInEditor(doc);
    }
    
    @Override
    public void onFailure(Throwable cause) {
        if (cause instanceof ServerException) {
            ServerException e = (ServerException)cause;
            if (e.getStatusCode() == 401) {
                DialogBox box = new DialogBox();
                box.setSize("400", "300");
                box.setTitle("Login");
                box.show();
                // TODO new LoginCommand().execute();
                return;
            }
            super.onFailure(cause);
        }
        
    }   
    
}
