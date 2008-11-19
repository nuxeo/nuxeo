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

package org.nuxeo.webengine.gwt.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.webengine.gwt.client.impl.SessionImpl;
import org.nuxeo.webengine.gwt.client.ui.ApplicationWindow;
import org.nuxeo.webengine.gwt.client.ui.impl.ApplicationWindowImpl;

import com.google.gwt.core.client.GWT;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Application {

    public static final String APPLICATION_WINDOW_XP = "APPLICATION_WINDOW";
    public static final String APPLICATION_SESSION_XP = "APPLICATION_SESSION";
    
    protected static List<SessionListener> sessionListeners = new ArrayList<SessionListener>();
    protected static Map<String, Extensible> extensionPoints = new HashMap<String, Extensible>(); 
    protected static ApplicationWindow perspective;
    protected static Session session;

    public static ApplicationWindow getPerspective() {
        return perspective;
    }
    
    public static void addSessionListener(SessionListener listener) {
        sessionListeners.add(listener);
    }
    
    public static SessionListener[] getSessionListeners() {
        return sessionListeners.toArray(new SessionListener[sessionListeners.size()]);
    }
    
    
    public static void login(String username, String password) {
        if (session.login(username, password)) {            
            for (SessionListener listener : sessionListeners) {
                listener.onSessionEvent(SessionListener.LOGIN);
            }
        }
    }
    
    public static void logout() {
        if (session.logout()) {
            for (SessionListener listener : sessionListeners) {
                listener.onSessionEvent(SessionListener.LOGOUT);
            }            
        }
    }
    
    public static void setInput(String url) {
        if (session.setInput(url)) {
            for (SessionListener listener : sessionListeners) {
                listener.onSessionEvent(SessionListener.INPUT);
            }  
        }
    }
    
    public static Session getSession() {
        return session;
    }
    
    public static String getUsername() {
        return session.getUsername();
    }
    
    public static Object getInput() {
        return session.getInput();
    }
    
    public static boolean isAuthenticated() {
        return session.isAuthenticated();
    }
    
    
    public static void registerExtensionPoint(String name, Extensible extensible) {
        extensionPoints.put(name, extensible);
    }
    
    public static void registerExtension(String extensionPoint, Object extension) {
        Extensible xp = extensionPoints.get(extensionPoint);
        if (xp != null) {
            xp.registerExtension(extensionPoint, extension);
        } else if (APPLICATION_WINDOW_XP.equals(extensionPoint)) {
            perspective =  (ApplicationWindow)extension;
        } else if (APPLICATION_SESSION_XP.equals(extensionPoint)) {
            session = (Session)extension;
        } else {
            GWT.log("Unknown Extension Point: "+extensionPoint, null);
        }
    }
    
    
    public static void start() {
        if (session != null) {
            return; // already started
        }
        session = new SessionImpl(); //TODO use extension points
        if (perspective == null) {
            perspective = new ApplicationWindowImpl();
        }
        perspective.install();
    }    
    

}
