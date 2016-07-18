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
package org.nuxeo.ecm.core.event.pipe.local;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.annotation.Experimental;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.pipe.AbstractEventBundlePipe;
import org.nuxeo.ecm.core.event.pipe.EventBundlePipe;

/**
 * Local In memory implementation: directly relays to WorkManager
 *
 * @since 8.4
 */
@Experimental
public class LocalEventBundlePipe extends AbstractEventBundlePipe<EventBundle> implements EventBundlePipe {

    protected LocalEventBundlePipeConsumer consumer;

    @Override
    public void initPipe(String name, Map<String, String> params) {
        super.initPipe(name, params);
        consumer = new LocalEventBundlePipeConsumer();
        consumer.initConsumer(name, params);
    }

    @Override
    protected void send(EventBundle message) {
        List<EventBundle> messages = new ArrayList<EventBundle>();
        messages.add(message);
        consumer.receiveMessage(messages);
    }

    @Override
    protected EventBundle marshall(EventBundle events) {
        return events;
    }

    @Override
    public boolean waitForCompletion(long timeoutMillis) throws InterruptedException {
        return consumer.waitForCompletion(timeoutMillis);
    }

}
