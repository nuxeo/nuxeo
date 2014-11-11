/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs.servlet.config;

import java.util.ArrayList;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("listeners")
public class ListenerSetDescriptor {

    @XNodeList(value="listener", type=ArrayList.class, componentType=ListenerDescriptor.class, trim=true, nullByDefault=false)
    protected ArrayList<ListenerDescriptor> listenerDescriptors;

    private ServletContextListener[] listeners;

    private int cnt = 0;

    public synchronized boolean isInitialized() {
        return cnt > 0;
    }

    public synchronized void init(ServletConfig config) throws Exception {
        cnt++;
        if (cnt == 1) { // first init
            ServletContextEvent event = new ServletContextEvent(config.getServletContext());
            listeners = new ServletContextListener[listenerDescriptors.size()];
            for (int i=0; i<listeners.length; i++) {
                ListenerDescriptor ld = listenerDescriptors.get(i);
                listeners[i] = ld.getListener();
                listeners[i].contextInitialized(event);
            }
        }
    }

    public synchronized boolean destroy(ServletConfig config) {
        if (cnt <= 0) { // should never happen
            return false;
        }
        cnt--;
        if (cnt == 0) {
            if (listeners != null) {
                try {
                    ServletContextEvent event = new ServletContextEvent(config.getServletContext());
                    for (ServletContextListener listener : listeners) {
                        listener.contextDestroyed(event);
                    }
                } finally {
                    listeners = null;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return listenerDescriptors.toString();
    }

}
