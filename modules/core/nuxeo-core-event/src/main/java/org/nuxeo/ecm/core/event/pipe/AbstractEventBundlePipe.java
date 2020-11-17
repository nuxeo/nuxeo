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

import org.nuxeo.ecm.core.event.EventBundle;

import java.util.Map;

/**
 * @since 8.4
 */
public abstract class AbstractEventBundlePipe<T> implements EventBundlePipe {

    protected String name;

    protected Map<String, String> params;

    @Override
    public void initPipe(String name, Map<String, String> params) {
        this.name = name;
        this.params = params;

        // XXX get contributions
    }

    protected String getName() {
        return name;
    }

    protected Map<String, String> getParameters() {
        return params;
    }

    @Override
    public void sendEventBundle(EventBundle events) {
        events = filterBundle(events);
        if (events.isEmpty()) {
            return;
        }
        preProcessBundle(events);
        send(marshall(events));
    }

    protected EventBundle filterBundle(EventBundle events) {
        // XXX handle contributions

        // remove events from bundles
        // remove bundles that are not interesting
        // typical use case : filter events before forwarding to an external bus

        return events;
    }

    protected void preProcessBundle(EventBundle events) {
        // XXX handle contributions

        // enrich events bundle
        // typical use case : resolve events extended attributes for Audit pipe

        return;
    }

    /**
     * de-hydrate the EventBundle to make it suitable for transmission on a Bus
     */
    protected abstract T marshall(EventBundle events);

    /**
     * Do the actual push on the Bus
     */
    protected abstract void send(T message);


    @Override
    public void shutdown() throws InterruptedException {

    }

}
