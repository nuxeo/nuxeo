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
package org.nuxeo.ecm.core.event.pipe.local;

import org.nuxeo.common.annotation.Experimental;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.pipe.AbstractEventBundlePipe;
import org.nuxeo.ecm.core.event.pipe.EventBundlePipe;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        List<EventBundle> messages = Collections.singletonList(message);
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
