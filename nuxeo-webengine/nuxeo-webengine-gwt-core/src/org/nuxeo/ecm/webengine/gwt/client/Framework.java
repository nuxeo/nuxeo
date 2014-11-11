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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Framework {

    public static final String APPLICATION_XP = "APPLICATION";

    protected static Map<String, Extensible> extensionPoints = new HashMap<String, Extensible>();
    protected static Map<String, List<Object>> waitingExtensions = new HashMap<String, List<Object>>();
    protected static Application application;
    protected static ErrorHandler errorHandler;
    protected static String basePath = null;
    protected static boolean isStarted = false;


    public static Application getApplication() {
        return application;
    }


    public static void setErrorHandler(ErrorHandler errorHandler) {
        Framework.errorHandler = errorHandler;
    }

    public static ErrorHandler getErrorHandler() {
        return errorHandler;
    }



    public static String getBasePath() {
        return basePath;
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

    public static String normalize(String uri) {
        if (basePath != null) {
            if (!uri.contains("://")) { // relative URL
                if (!uri.startsWith("/")) {
                    uri = basePath+uri;
                } else {
                    uri = basePath+uri.substring(1);
                }
            }
        }
        return URL.encode(uri);
    }

    public static void start(String url) {
        if (isStarted) {
            throw new IllegalStateException("Application already started!");
        }
        GWT.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            public void onUncaughtException(Throwable e) {
                Framework.handleError(e);
            }
        });
//        if (url != null && url.endsWith("/")) {
//            basePath = url.substring(0, url.length()-1);
//        } else {
//            basePath = url;
//        }
        basePath = url != null ? url : null;
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
