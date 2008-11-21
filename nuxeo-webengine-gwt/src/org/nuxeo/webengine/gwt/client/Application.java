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
import com.google.gwt.user.client.ui.ImageBundle;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Application {

    public static final String APPLICATION_WINDOW_XP = "APPLICATION_WINDOW";
    public static final String APPLICATION_SESSION_XP = "APPLICATION_SESSION";
    
    protected static List<ContextListener> sessionListeners = new ArrayList<ContextListener>();
    protected static Map<String, Extensible> extensionPoints = new HashMap<String, Extensible>();
    protected static Map<String, List<Object[]>> waitingExtensions = new HashMap<String, List<Object[]>>();
    protected static ApplicationWindow window;
    protected static Session session;
    protected static Context ctx = new Context();
    protected static Map<String, ImageBundle> images = new HashMap<String, ImageBundle>();
    
    public static ApplicationWindow getWindow() {
        return window;
    }
    
    public static void addContextListener(ContextListener listener) {
        sessionListeners.add(listener);
    }
    
    public static ContextListener[] getContextListeners() {
        return sessionListeners.toArray(new ContextListener[sessionListeners.size()]);
    }
    
    
    public static void login(String username, String password) {
        if (session.login(username, password)) {  
            ctx.setUsername(username);
        }
    }
    
    public static void logout() {
        if (session.logout()) {
            ctx.setUsername(null);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T getImages(Class<T> bundleClass) {
         ImageBundle bundle = images.get(bundleClass.getName());
         if (bundle == null) {
             bundle = GWT.create(bundleClass);
             images.put(bundleClass.getName(), bundle);
         }
         return (T)bundle;
    }
        
    
    public static Object load(String url) {
        Object input = session.get(url);
        if (input != null) {
            ctx.setInputObject(input);
        }
        return ctx.getInputObject();
    }
    
    protected static void fireEvent(int event) {
        for (ContextListener listener : Application.sessionListeners) {
            listener.onSessionEvent(event);
        }  
    }

    public static Session getSession() {
        return session;
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
        } else if (APPLICATION_WINDOW_XP.equals(extensionPoint)) {
            if (mode == Extension.ADD_IF_NOT_EXISTS && window != null) {
                return;
            }
            window =  (ApplicationWindow)extension;
        } else if (APPLICATION_SESSION_XP.equals(extensionPoint)) {
            if (mode == Extension.ADD_IF_NOT_EXISTS && session != null) {
                return;
            }
            session = (Session)extension;
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
    
    public static void debugStart(String url) {
        if (GWT.isScript()) {
            throw new IllegalStateException("Debug mode is available only in hosted mode");
        }        
        start();  
        try {
            load(url);
        } catch (Exception e) {
            e.printStackTrace();
            GWT.log("Failed to start in debug mode", e);
        }
    }
    
    public static void start() {
        if (session != null) {
            return; // already started
        }
        session = new SessionImpl(); //TODO use extension points
        if (window == null) {
            window = new ApplicationWindowImpl().register();
        }
        window.install();
        if (!waitingExtensions.isEmpty()) {//TODO
            GWT.log("There are extensions waiting to be deployed - "+waitingExtensions, null);
        }
    }    
    
}
