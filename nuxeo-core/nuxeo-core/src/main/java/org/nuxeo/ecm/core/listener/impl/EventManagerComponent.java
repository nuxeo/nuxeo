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

package org.nuxeo.ecm.core.listener.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.listener.CoreEventListenerService;
import org.nuxeo.ecm.core.listener.EventListener;
import org.nuxeo.ecm.core.listener.extensions.CoreEventListenerDescriptor;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class EventManagerComponent extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName(
        "org.nuxeo.ecm.core.listener.CoreEventListenerService");

    private static final Log log = LogFactory.getLog(EventManagerComponent.class);

    private CoreEventListenerService service;

    @Override
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);
        String klass = (String)context.getPropertyValue("class", null);
        if (klass == null) {
            service = new DefaultEventService();
        } else {
            service = (CoreEventListenerService)context
                .getRuntimeContext().loadClass(klass).newInstance();
        }
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        //service = null;
    }

    @Override
    public void registerExtension(Extension extension) throws Exception {
        super.registerExtension(extension);
        Object[] contributions = extension.getContributions();
        if (contributions != null) {
            if (extension.getExtensionPoint().equals("listener")) {
                for (Object contribution : contributions) {
                    CoreEventListenerDescriptor desc = (CoreEventListenerDescriptor) contribution;
                    EventListener listener = (EventListener) extension.getContext().loadClass(
                            desc.getClassName()).newInstance();
                    listener.setName(desc.getName());
                    listener.setOrder(desc.getOrder());
                    String[] eventIds = desc.getEventIds();
                    if (eventIds != null) {
                        for (String eventId : eventIds) {
                            listener.addEventId(eventId);
                        }
                    }
                    service.addEventListener(listener);
                    log.info("Registered core event listener: " +
                            desc.getName());
                }
            }
        }
    }

    @Override
    public void unregisterExtension(Extension extension) throws Exception {
        Object[] contributions = extension.getContributions();
        if (contributions != null) {
            if (extension.getExtensionPoint().equals("listener")) {
                for (Object contribution : contributions) {
                    CoreEventListenerDescriptor desc = (CoreEventListenerDescriptor) contribution;
                    service.removeEventListener(service.getEventListenerByName(desc.getName()));
                    log.info("Unregistered core event listener: " +
                            desc.getName());
                }
            }
        }
        super.unregisterExtension(extension);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == CoreEventListenerService.class) {
            return adapter.cast(service);
        }
        return null;
    }

    public CoreEventListenerService getEventService() {
        return service;
    }

}
