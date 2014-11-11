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

package org.nuxeo.ecm.platform.gwt.client;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Framework {

    public static final String APPLICATION_XP = "APPLICATION";
    public static final String HISTORY_LISTENER_XP = "HISTORY_LISTENER";
    public static final String JS_HANDLER_XP = "JS_HANDLER";
    
    protected static Map<String, Extensible> extensionPoints = new HashMap<String, Extensible>();
    protected static Map<String, List<Object>> waitingExtensions = new HashMap<String, List<Object>>();
    protected static Application application;
    protected static ErrorHandler errorHandler;
    protected static boolean isStarted = false;
    protected static Map<String, JSHandler> jsHandlers = new HashMap<String, JSHandler>();
    
    
    public static Application getApplication() {
        return application;
    }
    
    
    public static void setErrorHandler(ErrorHandler errorHandler) {
        Framework.errorHandler = errorHandler;
    }
    
    public static ErrorHandler getErrorHandler() {
        return errorHandler;
    }
    

    public static void handleError(Throwable t) {
        GWT.log(t.getMessage(), t);
        if (!GWT.isScript()) {
            t.printStackTrace();
        }
        if (errorHandler != null) {
            errorHandler.handleError(t);
        } else {
            Window.alert("Uncaught Ecxception: "+t.getMessage());
        }
    }
    
    public static void registerExtensionPoint(String name, Extensible extensible) {
        extensionPoints.put(name, extensible);
        List<Object> list = waitingExtensions.remove(name);
        if (list != null) {
            for (Object entry : list) {
                extensible.registerExtension(name.toString(), entry);
            }
        }
    }
    
    public static void registerExtension(String extensionPoint, Object extension) {
        Extensible xp = extensionPoints.get(extensionPoint);
        if (xp != null) {
            xp.registerExtension(extensionPoint, extension);
        } else if (APPLICATION_XP.equals(extensionPoint)) {
            application =  (Application)extension;
        } else if (HISTORY_LISTENER_XP.equals(extensionPoint)) {
            History.addHistoryListener((HistoryListener)extension);
        } else if (JS_HANDLER_XP.equals(extensionPoint)) {            
            //TODO registerJSHandler(, handler)((JSHandler)extension);
        } else {
            List<Object> list = waitingExtensions.get(extensionPoint);
            if (list == null) {
                list = new ArrayList<Object>();
                waitingExtensions.put(extensionPoint, list);
            }
            list.add(extension);
            GWT.log("Postpone extension registration for: "+extensionPoint, null);
        }
    }
    
    public static void start(String url) {
        try {
            showLoading("Starting ...");
            doStart(url);
        } catch (Throwable t) {
            GWT.log(t.getMessage(), t);
            Window.alert("Error: "+t.getMessage());
        } finally {
            // hide loading message
            showLoading(null);            
        }
    }
    
    public static void doStart(String url) {
        if (isStarted) {
            throw new IllegalStateException("Application already started!");
        }
        GWT.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            public void onUncaughtException(Throwable e) {
                Framework.handleError(e);
            }
        });
        // force JS context creation 
        JSContext.getCurrent();
        
        if (application == null) {
            GWT.log("You must define an application!", null);
            throw new IllegalStateException("There is no application to start!");
        }
        application.start();        
        if (!waitingExtensions.isEmpty()) {//TODO use onAtach in application to clear the map? 
            GWT.log("There are extensions waiting to be deployed - "+waitingExtensions, null);
        }
        History.fireCurrentHistoryState();
    }
    
    public static void start() {
        start(null);
    }    

    

    
    public static void registerJSHandler(String id, JSHandler handler) {
        jsHandlers.put(id, handler);
    }
    
    public static void unregisterJSHandler(String id) {
        jsHandlers.remove(id);
    }
    
    
    public static Object handleJSEvent(String eventId, String data) {
        JSHandler handler = jsHandlers.get(eventId);
        if (handler == null) {
            Window.alert("No handlers registered for JS event: "+eventId);
            return null;
        } else {
            return handler.onEvent(data);
        }
    }
    
    public static JSContext getJSContext() {
        return JSContext.getCurrent();
    }
    
    public static String getSkinPath(String path) {
        if (path.startsWith("/")) {
            return JSContext.getCurrent().getSkinPath()+path;    
        }       
        return JSContext.getCurrent().getSkinPath()+"/"+path;
    }

    public static String getResourcePath(String path) {
        if (path.startsWith("/")) {
            return JSContext.getCurrent().getModulePath()+path;    
        }       
        return JSContext.getCurrent().getModulePath()+"/"+path;
    }

    public static String getModulePath() {
        return JSContext.getCurrent().getModulePath();
    }
    
    public static String getSkinPath() {
        return JSContext.getCurrent().getSkinPath();
    }
    
    public static String getUserName() {
        return JSContext.getCurrent().getUserName();
    }
    
    public static String getAnonymousUserName() {
        return JSContext.getCurrent().getAnonymousUserName();
    }    
    
    public static Map<String, String> getRepositoryRoots() {
        return JSContext.getCurrent().getRepositoryRoots();
    }    
    
    public static String getSetting(String key) {
        return JSContext.getCurrent().getProperty(key);
    }    
    
    public static String getSetting(String key, String defValue) {
        String val = getSetting(key);
        return val == null ? defValue : val;
    }
    
    /** loading dialog */
    public static void showLoading(String text) {
        RootPanel loading = RootPanel.get("loading");
        if (loading != null) {
            if (text == null) {
                loading.setVisible(false);                 
            } else {
                RootPanel msg = RootPanel.get("loadingMsg");
                if (msg != null) {
                    msg.getElement().setInnerHTML(text);
                }
                loading.setVisible(true);
            }
        }                
    }
    

}
