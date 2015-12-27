/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.event.test;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;

public class DummyPostCommitEventListener implements PostCommitEventListener {

    private static AtomicInteger handledCount = new AtomicInteger(0);

    private static AtomicInteger eventCount = new AtomicInteger(0);

    private static AtomicInteger interruptCount = new AtomicInteger(0);

    public static volatile Map<String, Serializable> properties;

    @Override
    public void handleEvent(EventBundle events) {
        handledCount.incrementAndGet();
        eventCount.addAndGet(events.size());
        // get properties from first event context
        properties = events.peek().getContext().getProperties();

        if (properties.get("throw") != null) {
            throw new NuxeoException("testing error case");
        }
        if (properties.get("sleep") != null) {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                interruptCount.incrementAndGet();
            }
        }
        if (properties.get("concurrentexception") != null && handledCount() == 1) {
            throw new ConcurrentUpdateException();
        }
    }

    public static int handledCount() {
        return handledCount.get();
    }

    public static int eventCount() {
        return eventCount.get();
    }

    public static int interruptCount() {
        return interruptCount.get();
    }

    public static void handledCountReset() {
        handledCount.set(0);
    }

    public static void eventCountReset() {
        eventCount.set(0);
    }

    public static void interruptCountReset() {
        interruptCount.set(0);
    }

}
