/*
 * (C) Copyright 2007-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Gethin James
 */
package org.nuxeo.ecm.core.event;

import java.util.concurrent.atomic.AtomicInteger;

import org.nuxeo.ecm.core.work.AbstractWork;

/**
 * An event listener that counts failed work.
 *
 * @since 10.1
 */
public class WorkFailureEventListener implements EventListener {

    protected static AtomicInteger count = new AtomicInteger(0);

    @Override
    public void handleEvent(Event event) {
        if (AbstractWork.WORK_FAILED_EVENT.equals(event.getName())) {
            count.incrementAndGet();
        }
    }

    public static int getCount() {
        return count.get();
    }

}
