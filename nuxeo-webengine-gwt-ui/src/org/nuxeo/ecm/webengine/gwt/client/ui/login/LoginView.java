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

package org.nuxeo.ecm.webengine.gwt.client.ui.login;

import org.nuxeo.ecm.webengine.gwt.client.Framework;
import org.nuxeo.ecm.webengine.gwt.client.http.HttpRequest;
import org.nuxeo.ecm.webengine.gwt.client.http.HttpResponse;
import org.nuxeo.ecm.webengine.gwt.client.ui.Item;

import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class LoginView extends Item {
    
    protected DeckPanel deck;
    
    public LoginView() {
        super("login");
        setTitle("Login");
        setPreferredIndex(100);
    }
    
    
    @Override
    protected Widget createContent() {
        deck = new DeckPanel();
        deck.add(new LoginWidget(this));
        deck.add(new LogoutWidget(this));
        deck.showWidget(0);
        System.out.println("login view created");
        return deck;
    }
    
    
    public void refresh() {    
        int index = deck.getVisibleWidget();
        if (Framework.isAuthenticated()) {
            if (index == 0) { // login widget is visible
                ((LogoutWidget)deck.getWidget(1)).refresh();
                deck.showWidget(1);                
            }
        } else {
            if (index == 1) { // logout widget is visible
                deck.showWidget(0);
            }            
        }
    }

    
    public void login(final String username, final String password) {
        try {
            get("/skin/wiki/wiki.css").setUser(username).setPassword(password).send();
        } catch (Exception e) {
            Framework.handleError(e);
        }
//        HttpCallback cb = new HttpCallback() {
//            @Override
//            public void onSuccess(HttpResponse response) {                
//                hideBusy();
//                Framework.getContext().setUsername(username);
//            }            
//            @Override
//            public void onFailure(Throwable cause) {
//                GWT.log("Failed to complete async request on server: "+cause.getMessage(), cause);
//                hideBusy(); 
//                if (cause instanceof ServerException) {
//                    int status = ((ServerException)cause).getStatusCode(); 
//                    if (status == 401) {
//                        System.out.println("login error");
//                        return;
//                    }
//                }
//                super.onFailure(cause);
//            }            
//        };
//        try {
//            showBusy();
//            //for (int i=0; i<1000;i++) { System.out.println("wwwwwwwwwwwwwww");}
//            Server.post("/skin/wiki/wiki.css").setUser(username).setPassword(password).setCallback(cb).send();
//        } catch (Exception e) {
//            hideBusy();
//            Framework.handleError(e);
//        }
    }
    
    public void logout() {
        try {
            get("/skin/wiki/wiki.css").send();
        } catch (Throwable t) {
            Framework.handleError(t);
        }        
    }
    
    @Override
    public void onRequestCompleted(HttpRequest request, HttpResponse response) {        
        Framework.getContext().setUsername(request.getUser());
    }
    
}
