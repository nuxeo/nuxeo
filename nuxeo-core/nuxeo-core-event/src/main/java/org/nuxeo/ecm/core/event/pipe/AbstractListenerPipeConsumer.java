/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     tiry
 */
package org.nuxeo.ecm.core.event.pipe;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.event.impl.AsyncEventExecutor;
import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;
import org.nuxeo.ecm.core.event.impl.EventListenerList;
import org.nuxeo.runtime.api.Framework;

import java.util.List;
import java.util.Map;

/**
 * Consumes {@link EventBundle} EventBundles by running asynchronous {@link org.nuxeo.ecm.core.event.EventListener}
 *
 * @since 8.4
 */
public abstract class AbstractListenerPipeConsumer<T> extends AbstractPipeConsumer<T> {

    private static final Log log = LogFactory.getLog(AbstractListenerPipeConsumer.class);

    protected volatile AsyncEventExecutor asyncExec;

    protected boolean stopping;

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
            EventServiceAdmin eventService = Framework.getService(EventServiceAdmin.class);//
            EventListenerList listeners = eventService.getListenerList();
            List<EventListenerDescriptor> postCommitAsync = listeners.getEnabledAsyncPostCommitListenersDescriptors();

            // could introduce bulk mode for EventListeners
            for (EventBundle eventBundle : bundles) {
                asyncExec.run(postCommitAsync, eventBundle);
            }
            return true;
    }

    @Override
    public boolean waitForCompletion(long timeoutMillis) throws InterruptedException {
        return asyncExec.waitForCompletion(timeoutMillis);
    }
}
