/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.nuxeo.ecm.webengine.jaxrs.BundleNotFoundException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("listeners")
public class ListenerSetDescriptor {

    @XNodeList(value = "listener", type = ArrayList.class, componentType = ListenerDescriptor.class, trim = true, nullByDefault = false)
    protected ArrayList<ListenerDescriptor> listenerDescriptors;

    private ServletContextListener[] listeners;

    private ServletContextEvent event;

    public synchronized boolean isInitialized() {
        return event != null;
    }

    public synchronized void init(ServletConfig config) throws ReflectiveOperationException, BundleNotFoundException {
        if (event == null && !listenerDescriptors.isEmpty()) {
            event = new ServletContextEvent(config.getServletContext());
            listeners = new ServletContextListener[listenerDescriptors.size()];
            for (int i = 0; i < listeners.length; i++) {
                ListenerDescriptor ld = listenerDescriptors.get(i);
                listeners[i] = ld.getListener();
                listeners[i].contextInitialized(event);
            }
        }
    }

    public synchronized boolean destroy() {
        if (event != null) {
            if (listeners != null) {
                try {
                    for (ServletContextListener listener : listeners) {
                        listener.contextDestroyed(event);
                    }
                } finally {
                    event = null;
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
