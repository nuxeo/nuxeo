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

package org.nuxeo.ecm.platform.gwt.client.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Context extends HashMap<String,Object> {

    private static final long serialVersionUID = 1L;

    protected Object input;
    protected static List<ContextListener> contextListeners = new ArrayList<ContextListener>();


    public Object getInputObject() {
        return input;
    }

    /**
     * @param object the object to set.
     */
    public void setInputObject(Object object) {
        if (this.input != object) {
            this.input = object;
            fireEvent(new ContextEvent(ContextEvent.INPUT, object));
        }
    }

    @Override
    public Object put(String key, Object o ) {
        Object obj = super.put(key, o);
        fireEvent(new ContextEvent(ContextEvent.PROP, key));
        return obj;
    }


    public static void fireEvent(ContextEvent event) {
        for (ContextListener listener : contextListeners) {
            listener.onContextChanged(event);
        }  
    }    

    public static void addContextListener(ContextListener listener) {
        contextListeners.add(listener);
    }
    
    public static void removeContextListener(ContextListener listener) {
        contextListeners.remove(listener);
    }    
    
    public static ContextListener[] getContextListeners() {
        return contextListeners.toArray(new ContextListener[contextListeners.size()]);
    }
    
}
