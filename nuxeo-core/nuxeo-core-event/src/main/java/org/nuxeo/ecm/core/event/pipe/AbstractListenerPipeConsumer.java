/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     tiry
 */
package org.nuxeo.ecm.core.event.pipe;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.event.impl.AsyncEventExecutor;
import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;
import org.nuxeo.ecm.core.event.impl.EventListenerList;
import org.nuxeo.runtime.api.Framework;

/**
 * Consumes EventBundles by running AsynchronousListeners
 *
 * @since TODO
 */
public abstract class AbstractListenerPipeConsumer<T> extends AbstractPipeConsumer<T> {


    protected volatile AsyncEventExecutor asyncExec;

    @Override
    public void initConsumer(String name, Map<String, String> params) {
        super.initConsumer(name, params);
        asyncExec = new AsyncEventExecutor();
    }

    @Override
    protected void processEventBundles(List<EventBundle> bundles) {

        EventServiceAdmin eventService = Framework.getService(EventServiceAdmin.class);
        EventListenerList listeners = eventService.getListenerList();
        List<EventListenerDescriptor> postCommitAsync =listeners.getEnabledAsyncPostCommitListenersDescriptors();

        // could introduce bulk mode for EventListeners
        for (EventBundle eventBundle : bundles) {
            asyncExec.run(postCommitAsync, eventBundle);
        }
    }

}
