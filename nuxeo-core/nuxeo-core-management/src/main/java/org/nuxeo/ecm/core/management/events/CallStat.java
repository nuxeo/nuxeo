/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.management.events;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple class to store Listeners call statistics.
 *
 * @author Thierry Delprat
 */
public class CallStat {

    protected AtomicLong accumulatedTime = new AtomicLong();

    protected AtomicInteger callCount = new AtomicInteger();

    protected final String label;

    public CallStat(String label) {
        this.label = label;
    }

    void update(long delta) {
        callCount.incrementAndGet();
        accumulatedTime.addAndGet(delta);
    }

    public long getAccumulatedTime() {
        return accumulatedTime.get();
    }

    public int getCallCount() {
        return callCount.get();
    }

    public String getLabel() {
        return label;
    }

}
