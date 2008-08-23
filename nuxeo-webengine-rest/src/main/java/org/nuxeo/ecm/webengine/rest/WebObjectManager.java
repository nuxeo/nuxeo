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

package org.nuxeo.ecm.webengine.rest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.ecm.webengine.rest.adapters.WebObject;


/**
 * THis is a temporary implementation. It will find obejct implementation in
 * package org.nuxeo.ecm.webengine.rest.adapters
 * To add a new object you must put there a class having its type name+Object.
 * Example for the Wiki type you should create a class
 * org.nuxeo.ecm.webengine.rest.adapters.WikiObject
 *
 *This will be refactored later to use annotations
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebObjectManager {

    private final static WebObjectManager instance = new WebObjectManager();

    //TODO implement it as a service
    public static WebObjectManager getCurrent() {
        return instance;
    }

    protected Map<String, Class<WebObject>> classes;

    public WebObjectManager() {
        classes = new ConcurrentHashMap<String, Class<WebObject>>();
    }

    @SuppressWarnings("unchecked")
    public WebObject newInstance(String type) {
        try {
            Class<WebObject> klass = classes.get(type);
            if (klass == null) {
                String name = "org.nuxeo.ecm.webengine.rest.adapters."+type+"Object";
                klass = (Class<WebObject>)Class.forName(name);
                classes.put(type, klass);
            }
            return klass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();//TODO
        }
        return null;
    }

    public void register(String type, Class<WebObject> klass) {
            classes.put(type, klass);
    }

    public void unregister(String type) {
        classes.remove(type);
    }

    public void clear() {
        classes.clear();
    }

    @SuppressWarnings("unchecked")
    public Class<WebObject>[] getClasses() {
        return classes.values().toArray(new Class[classes.size()]);
    }

}
