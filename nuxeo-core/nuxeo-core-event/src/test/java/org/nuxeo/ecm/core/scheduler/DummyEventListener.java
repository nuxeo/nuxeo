/*
 * (C) Copyright 2007-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.scheduler;

import java.util.concurrent.atomic.AtomicLong;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;

public class DummyEventListener implements EventListener {

    protected static AtomicLong count = new AtomicLong(0);

    protected static AtomicLong newCount = new AtomicLong(0);

    @Override
    public void handleEvent(Event event) throws ClientException {
        if (event.getName().equals("testEvent")) {
            String flag = (String) event.getContext().getProperty("flag");
            if ("1".equals(flag)) {
                count.decrementAndGet();
            } else {
                count.incrementAndGet();
            }
        }
        if ("testNewEvent".equals(event.getName())) {
            newCount.incrementAndGet();
        }
    }

    protected static void setCount(long val) {
        count.set(val);
    }

    protected static long getCount() {
        return count.get();
    }

    protected static long getNewCount() {
        return newCount.get();
    }
}
