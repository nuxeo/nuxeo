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

import java.util.Map;

import org.nuxeo.ecm.core.event.EventBundle;

/**
 * @since TODO
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
        if (events.size() == 0) {
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
     * dehydrate the EventBundle to make it suitable for transmission on a Bus
     *
     * @param events
     * @return
     */
    protected abstract T marshall(EventBundle events);

    /**
     * Do the actual push on the Bus
     *
     * @param message
     */
    protected abstract void send(T message);


    @Override
    public void shutdown() throws InterruptedException {

    }

}
