/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.common.xmap;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Context extends ArrayList<Object> {

    private static final long serialVersionUID = 1L;

    // Declare as a HashMap so that it is considered serializable
    @SuppressWarnings({"CollectionDeclaredAsConcreteClass"})
    private final HashMap<String, Object> properties = new HashMap<String, Object>();


    public Class loadClass(String className) throws ClassNotFoundException {
        return Thread.currentThread().getContextClassLoader().loadClass(className);
    }

    public URL getResource(String name) {
        return Thread.currentThread().getContextClassLoader().getResource(name);
    }

    public Object getObject() {
        int size = size();
        if (size > 0) {
            return get(size - 1);
        }
        return null;
    }

    public Object getParent() {
        int size = size();
        if (size > 1) {
            return get(size - 2);
        }
        return null;
    }

    public void push(Object object) {
        add(object);
    }

    public Object pop() {
        int size = size();
        if (size > 0) {
            return remove(size - 1);
        }
        return null;
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

}
