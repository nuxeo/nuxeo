/*
 * (C) Copyright 2012-2018 Nuxeo (http://nuxeo.com/) and others.
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

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Simple work that just sleeps, mostly used for tests.
 */
public class SleepWork extends AbstractWork {
    
    private static final Logger log = LogManager.getLogger(SleepWork.class);

    public static final String CATEGORY = "SleepWork";

    private static final long serialVersionUID = 1L;

    protected long durationMillis;

    protected String category;

    protected AtomicInteger count = new AtomicInteger();

    protected String partitionKey;

    protected boolean idempotent = true;

    protected boolean coalescing = false;

    /**
     * Creates a work instance that does nothing but sleep.
     *
     * @param durationMillis the sleep duration
     * @since 10.2
     */
    public SleepWork(long durationMillis, String category, String id) {
        super(id);
        this.durationMillis = durationMillis;
        this.category = category;
        partitionKey = String.valueOf(count.incrementAndGet());
        setProgress(Progress.PROGRESS_0_PC);
    }

    /**
     * @since 10.2
     */
    public SleepWork(long durationMillis, String id) {
        this(durationMillis, CATEGORY, id);
    }

    /**
     * @since 10.2
     */
    public SleepWork(long durationMillis) {
        this(durationMillis, CATEGORY, UUID.randomUUID().toString());
    }

    /**
     * If debug is true, then the various debug* methods must be called in the proper order for the work to start and
     * stop: {@link #debugStart}, {@link #debugFinish}.
     *
     * @param durationMillis the sleep duration
     * @param debug {@code true} for debug
     * @deprecated since 10.2 debug flag is unused
     */
    @Deprecated
    public SleepWork(long durationMillis, boolean debug) {
        this(durationMillis, null, CATEGORY);
    }

    /**
     * @deprecated since 10.2 debug flag is unused
     */
    @Deprecated
    public SleepWork(long durationMillis, boolean debug, String id) {
        this(durationMillis, null, id);
    }

    /**
     * @deprecated since 10.2 debug flag is unused
     */
    @Deprecated
    public SleepWork(long durationMillis, String category, boolean debug) {
        this(durationMillis, category, CATEGORY);
    }

    /**
     * @deprecated since 10.2 debug flag is unused
     */
    @Deprecated
    public SleepWork(long durationMillis, String category, boolean debug, String id) {
        this(durationMillis, category, id);
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public String getTitle() {
        return "Sleep " + durationMillis + " ms";
    }

    @Override
    public void work() {
        log.debug("Running " + this);
        try {
            doWork();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    protected void doWork() throws InterruptedException {
        for (;;) {
            long elapsed = System.currentTimeMillis() - getStartTime();
            if (elapsed > durationMillis) {
                break;
            }
            setProgress(new Progress(100F * elapsed / durationMillis));

            if (isSuspending()) {
                durationMillis -= elapsed; // save state
                suspended();
                return;
            }

            if (WorkStateHelper.isCanceled(getId())) {
                durationMillis -= elapsed; // save state
                return;
            }

            Thread.sleep(10);
        }

    }

    @Override
    public String getPartitionKey() {
        return partitionKey;
    }

    @Override
    public boolean isIdempotent() {
        return idempotent;
    }

    public void setIdempotent(boolean idempotent) {
        this.idempotent = idempotent;
    }

    @Override
    public boolean isCoalescing() {
        return coalescing;
    }

    public void setCoalescing(boolean coalescing) {
        this.coalescing = coalescing;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + (getId().length() > 10 ? "" : getId() + ", ") + durationMillis
                + "ms, " + getProgress() + ")";
    }

}
