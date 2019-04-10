/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.importer.queue;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @since 8.3
 */
public abstract class AbstractTaskRunner implements TaskRunner {

    protected volatile boolean completed = false;

    protected volatile Exception error;

    protected final AtomicLong nbProcessed = new AtomicLong(0);

    protected volatile boolean mustStop;

    protected volatile boolean canStop;

    protected volatile boolean started = false;


    protected void incrementProcessed() {
        nbProcessed.incrementAndGet();
    }

    @Override
    public boolean isCompleted() {
        return completed;
    }

    @Override
    public boolean isTerminated() {
        return !started || (completed || getError() != null);
    }

    @Override
    public Exception getError() {
        return error;
    }

    @Override
    public long getNbProcessed() {
        return nbProcessed.get();
    }

    @Override
    public void mustStop() {
        canStop = true;
        mustStop = true;
        started = false;
    }

    @Override
    public void canStop() {
        canStop = true;
    }

}
