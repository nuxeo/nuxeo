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

package org.nuxeo.ecm.webengine.gwt.client;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.webengine.gwt.client.impl.ServerImpl;

import com.google.gwt.core.client.GWT;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Framework {

    public static final String APPLICATION_XP = "APPLICATION_WINDOW";
    public static final String SERVER_XP = "APPLICATION_SESSION";
    
    protected static List<ContextListener> sessionListeners = new ArrayList<ContextListener>();
    protected static Map<String, Extensible> extensionPoints = new HashMap<String, Extensible>();
    protected static Map<String, List<Object[]>> waitingExtensions = new HashMap<String, List<Object[]>>();
    protected static Application application;
    protected static Server server;
    protected static Context ctx = new Context();    
    
    public static Application getApplication() {
        return application;
    }
    
    public static void addContextListener(ContextListener listener) {
        sessionListeners.add(listener);
    }
    
    public static ContextListener[] getContextListeners() {
        return sessionListeners.toArray(new ContextListener[sessionListeners.size()]);
    }
    
    
    public static void login(String username, String password) {
        if (server.login(username, password)) {  
            ctx.setUsername(username);
        }
    }
    
    public static void logout() {
        if (server.logout()) {
            ctx.setUsername(null);
        }
    }
    
    
    
    protected static void fireEvent(int event) {
        for (ContextListener listener : Framework.sessionListeners) {
            listener.onSessionEvent(event);
        }  
    }

    public static Server getServer() {
        return server;
    }
    
    public static boolean isAuthenticated() {
        return ctx.username != null;
    }    
    
    public static Context getContext() {
        return ctx;
    }
    
    public static void registerExtensionPoint(String name, Extensible extensible) {
        extensionPoints.put(name, extensible);
        List<Object[]> list = waitingExtensions.remove(name);
        if (list != null) {
            for (Object[] entry : list) {
                extensible.registerExtension(name.toString(), entry[0], ((Integer)entry[1]).intValue());
            }
        }
    }
    
    public static void registerExtension(String extensionPoint, Object extension) {
        registerExtension(extensionPoint, extension, Extension.APPEND);
    }

    public static void registerExtension(String extensionPoint, Object extension, int mode) {
        Extensible xp = extensionPoints.get(extensionPoint);
        if (xp != null) {
            xp.registerExtension(extensionPoint, extension, mode);
        } else if (APPLICATION_XP.equals(extensionPoint)) {
            if (mode == Extension.ADD_IF_NOT_EXISTS && application != null) {
                return;
            }
            application =  (Application)extension;
        } else if (SERVER_XP.equals(extensionPoint)) {
            if (mode == Extension.ADD_IF_NOT_EXISTS && server != null) {
                return;
            }
            server = (Server)extension;
        } else {
            List<Object[]> list = waitingExtensions.get(extensionPoint);
            if (list == null) {
                list = new ArrayList<Object[]>();
                waitingExtensions.put(extensionPoint, list);
            }
            list.add(new Object[] {extension, new Integer(mode)});
            GWT.log("Postpone extension registration for: "+extensionPoint, null);
        }
    }
    

    public static void start(String url) {
        if (server != null) {
            return; // already started
        }
        server = new ServerImpl(url); //TODO use extension points
        if (application == null) {
            GWT.log("You must define an application!", null);
            throw new IllegalStateException("There is no application to start!");
        }
        application.start();
        if (!waitingExtensions.isEmpty()) {//TODO use onAtach in application to clear the map? 
            GWT.log("There are extensions waiting to be deployed - "+waitingExtensions, null);
        }
    }
    
    public static void start() {
        start(null);
    }    
    
}
