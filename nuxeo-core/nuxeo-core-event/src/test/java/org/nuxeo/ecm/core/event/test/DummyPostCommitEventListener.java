/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;

public class DummyPostCommitEventListener implements PostCommitEventListener {

    private static AtomicInteger handledCount = new AtomicInteger(0);

    private static AtomicInteger eventCount = new AtomicInteger(0);

    private static AtomicInteger interruptCount = new AtomicInteger(0);

    public static volatile Map<String, Serializable> properties;

    @Override
    public void handleEvent(EventBundle events) throws ClientException {
        handledCount.incrementAndGet();
        eventCount.addAndGet(events.size());
        // get properties from first event context
        properties = events.peek().getContext().getProperties();

        if (properties.get("throw") != null) {
            throw new ClientException("testing error case");
        }
        if (properties.get("sleep") != null) {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                interruptCount.incrementAndGet();
            }
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
