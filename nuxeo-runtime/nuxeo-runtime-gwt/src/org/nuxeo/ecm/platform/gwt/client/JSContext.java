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

import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * A Java overlay type over a JS native type.
 *  
 * This native type is the one used to propagate configuration and events from JS code to java code.
 * 
 *   
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JSContext extends JavaScriptObject {

    private static JSContext current = createContext();
    
    private native static JSContext createContext()/*-{
    if ($wnd.nx === undefined) {
        $wnd.nx = {}; 
    }    
    $wnd.nx.fire = function (eventId, data) { 
        return @org.nuxeo.ecm.platform.gwt.client.Framework::handleJSEvent(Ljava/lang/String;Ljava/lang/String;)(eventId, data); 
    };
    return $wnd.nx;
    }-*/;

    public static JSContext getCurrent() {
        return current;
    }
    
    
    // Overlay types always have protected, zero-arg constructors
    protected JSContext() { }
    
    //TODO: we really want to expose fire? May be a sendEvent to JS is better to expose there 
    public final native Object fire(String eventId, String data) /*-{
        return this.fire(eventId, data);
    }-*/;
    
    public final native String getVersion() /*-{
        return this.version;
    }-*/;
    
    public final native String getSkinPath() /*-{
        return this.skinPath;
    }-*/;

    public final native String getModulePath() /*-{
        return this.modulePath;
    }-*/;
    
    public final native String getUserName() /*-{
        return this.userName;
    }-*/;

    public final native String getAnonymousUserName() /*-{
        return this.anonymousUserName;
    }-*/;
    
    public final native String getProperty(String key) /*-{
        return this.settings[key];
    }-*/;

  
    public final native Map<String, String> getRepositoryRoots() /*-{
        var map = @java.util.HashMap::new()();
        for (var key in this.repositoryRoots) {
            map.@java.util.HashMap::put(Ljava/lang/Object;Ljava/lang/Object;)(key, this.repositoryRoots[key]);
        }
        return map;
    }-*/;

        
}
