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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.event.impl.AsyncEventExecutor;
import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;
import org.nuxeo.ecm.core.event.impl.EventListenerList;
import org.nuxeo.runtime.api.Framework;

/**
 * Consumes EventBundles by running AsynchronousListeners
 *
 * @since 8.4
 */
public abstract class AbstractListenerPipeConsumer<T> extends AbstractPipeConsumer<T> {

    protected static final Log log = LogFactory.getLog(AbstractListenerPipeConsumer.class);

    protected volatile AsyncEventExecutor asyncExec;

    protected boolean stopping = false;

    @Override
    public void initConsumer(String name, Map<String, String> params) {
        super.initConsumer(name, params);
        asyncExec = new AsyncEventExecutor();
        if (Framework.getRuntime() == null) {
            throw new RuntimeException("Nuxeo Runtime not initialized");
        }
    }

    @Override
    public void shutdown() throws InterruptedException {
        stopping = true;
        waitForCompletion(1000L);
    }

    @Override
    protected boolean processEventBundles(List<EventBundle> bundles) {

        try {
            EventServiceAdmin eventService = Framework.getService(EventServiceAdmin.class);
            EventListenerList listeners = eventService.getListenerList();
            List<EventListenerDescriptor> postCommitAsync = listeners.getEnabledAsyncPostCommitListenersDescriptors();

            // could introduce bulk mode for EventListeners
            for (EventBundle eventBundle : bundles) {
                asyncExec.run(postCommitAsync, eventBundle);
            }
            return true;
        } catch (NullPointerException npe) {
            if (stopping) {
                log.warn("Trying to send events in pipe after shutdown");
            } else {
                log.error("Unable to acess Runtime in the context of EventPipe processing", npe);
            }
            return false;
        }
    }

    public boolean waitForCompletion(long timeoutMillis) throws InterruptedException {
        return asyncExec.waitForCompletion(timeoutMillis);
    }

}
