/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.webengine.jaxrs.servlet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.nuxeo.ecm.webengine.jaxrs.Utils;
import org.nuxeo.ecm.webengine.jaxrs.servlet.config.ServletRegistry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CompositeListener {

    protected static Map<String, CompositeListener> registry = new HashMap<String, CompositeListener>();

    public static CompositeListener getListeners(ServletConfig config) throws Exception {
        ServletContext ctx = config.getServletContext();
        String name = config.getInitParameter(ServletRegistry.SERVLET_NAME);
        if (name == null) {
            return null;
        }
        String classRefs = config.getInitParameter("listeners");
        if (classRefs == null) {
            return null;
        }
        classRefs = classRefs.trim();
        if (classRefs.length() == 0) {
            return null;
        }
        synchronized (CompositeListener.class) {
            CompositeListener listener = registry.get(name);
            if (listener == null) {
                listener = new CompositeListener(new ServletContextEvent(ctx), classRefs);
            }
            listener.cnt++;
            registry.put(name, listener);
            return listener;
        }
    }

    public static boolean destroyListeners(CompositeListener clistener) {
        ServletContext ctx = clistener.event.getServletContext();
        String name = ctx.getInitParameter(ServletRegistry.SERVLET_NAME);
        if (name == null) {
            return false;
        }
        synchronized (CompositeListener.class) {
            CompositeListener listener = registry.get(name);
            if (listener != null) {
                listener.cnt--;
                if (listener.cnt == 0) {
                    listener = registry.remove(name);
                    for (ServletContextListener l : listener.listeners) {
                        l.contextDestroyed(clistener.event);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    protected int cnt = 0;
    protected ServletContextEvent event;
    protected List<ServletContextListener> listeners = new ArrayList<ServletContextListener>();

    private CompositeListener(ServletContextEvent event, String classRefs) throws Exception {
        this.event = event;
        listeners = new ArrayList<ServletContextListener>();
        if (classRefs != null) {
            classRefs = classRefs.trim();
            for (Object o : Utils.newInstances(ServletContextListener.class, classRefs)) {
                ServletContextListener listener = (ServletContextListener)o;
                listeners.add(listener);
                listener.contextInitialized(event);
            }
        }
    }


    public ServletContextListener[] getListeners() {
        return listeners.toArray(new ServletContextListener[listeners.size()]);
    }

}
