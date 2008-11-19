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

package org.nuxeo.webengine.gwt.client.ui.login;

import org.nuxeo.webengine.gwt.client.Application;
import org.nuxeo.webengine.gwt.client.ContextListener;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DeckPanel;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class LoginContainer extends Composite implements ContextListener {
    
    protected DeckPanel deck;
    
    public LoginContainer() {
        deck = new DeckPanel();
        deck.add(new LoginWidget());
        deck.add(new LogoutWidget());
        deck.showWidget(0);
        initWidget(deck);
    }
    
    
    public void refresh() {    
        int index = deck.getVisibleWidget();
        if (Application.isAuthenticated()) {
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

    public void onSessionEvent(int event) {
        if (isVisible() && event == LOGIN || event == LOGOUT) {
            refresh();
        }
    }
    
}
