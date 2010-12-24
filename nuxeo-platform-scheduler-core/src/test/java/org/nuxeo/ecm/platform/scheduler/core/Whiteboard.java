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
package org.nuxeo.ecm.platform.scheduler.core;

import java.util.concurrent.atomic.AtomicLong;

public class Whiteboard {

    private static Whiteboard singleton;

    private AtomicLong count = new AtomicLong(0);

    // Utility class.
    private Whiteboard() {
    }

    public static synchronized Whiteboard getWhiteboard() {
        if (singleton == null) {
            singleton = new Whiteboard();
        }
        return singleton;
    }

    public void setCount(long val) {
        count.set(val);
    }

    public long getCount() {
        return count.get();
    }

    public void incrementCount() {
        count.incrementAndGet();
    }

    public synchronized void decreaseCount() {
        count.decrementAndGet();
    }

}
