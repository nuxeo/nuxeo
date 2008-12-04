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

import org.nuxeo.ecm.webengine.gwt.client.UI;
import org.nuxeo.ecm.webengine.gwt.client.ui.View;

import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class LoginView extends View {

    protected DeckPanel deck;

    public LoginView() {
        super("login");
        setTitle("Login");
    }


    @Override
    protected Widget createContent() {
        deck = new DeckPanel();
        deck.add(new LoginWidget());
        deck.add(new LogoutWidget());
        deck.showWidget(0);
        return deck;
    }


    public void refresh() {
        int index = deck.getVisibleWidget();
        if (UI.isAuthenticated()) {
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

}
