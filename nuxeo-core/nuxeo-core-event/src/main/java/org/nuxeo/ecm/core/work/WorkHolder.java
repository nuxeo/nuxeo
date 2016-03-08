/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.work;

import java.util.concurrent.ThreadPoolExecutor;

import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.runtime.trackers.concurrent.ThreadEvent;

/**
 * A {@link WorkHolder} adapts a {@link Work} to {@link Runnable} for queuing and execution by a
 * {@link ThreadPoolExecutor}.
 * <p>
 * Calls (indirectly) {@link Work#work} and {@link Work#cleanUp}.
 *
 * @see Work
 * @see Work#work
 * @see Work#cleanUp
 * @see AbstractWork
 * @since 5.8
 */
public class WorkHolder implements Runnable {

    private final Work work;

    public WorkHolder(Work work) {
        this.work = work;
    }

    public static Work getWork(Runnable r) {
        return ((WorkHolder) r).work;
    }

    @Override
    public void run() {
        final Thread currentThread = Thread.currentThread();
        String name = currentThread.getName();
        currentThread.setName(name + ":" + work.getId());
        ThreadEvent.onEnter(this, false).send();
        try {
            work.run();
        } finally {
            currentThread.setName(name);
            ThreadEvent.onLeave(this).send();
        }
    }

}
