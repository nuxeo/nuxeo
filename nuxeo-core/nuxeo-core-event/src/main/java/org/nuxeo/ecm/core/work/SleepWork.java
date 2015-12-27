/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.concurrent.CountDownLatch;

/**
 * Simple work that just sleeps, mostly used for tests.
 */
public class SleepWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    protected long durationMillis;

    protected String category;

    /** used for debug. */
    protected static transient boolean debug;

    /** used for debug. */
    protected static CountDownLatch readyLatch = new CountDownLatch(1);

    /** used for debug. */
    protected static CountDownLatch doneLatch = new CountDownLatch(1);

    /** used for debug. */
    protected static CountDownLatch startLatch = new CountDownLatch(1);

    /** used for debug. */
    protected static CountDownLatch finishLatch = new CountDownLatch(1);

    /**
     * Creates a work instance that does nothing but sleep.
     *
     * @param durationMillis the sleep duration
     */
    public SleepWork(long durationMillis) {
        this(durationMillis, "SleepWork", false);
    }

    /**
     * If debug is true, then the various debug* methods must be called in the proper order for the work to start and
     * stop: {@link #debugStart}, {@link #debugFinish}.
     *
     * @param durationMillis the sleep duration
     * @param debug {@code true} for debug
     */
    public SleepWork(long durationMillis, boolean debug) {
        this(durationMillis, "SleepWork", debug);
    }

    public SleepWork(long durationMillis, boolean debug, String id) {
        this(durationMillis, "SleepWork", debug, id);
    }

    public SleepWork(long durationMillis, String category, boolean debug) {
        super();
        init(durationMillis, category, debug);
    }

    public SleepWork(long durationMillis, String category, boolean debug, String id) {
        super(id);
        init(durationMillis, category, debug);
    }

    private void init(long durationMillis, String category, boolean debug) {
        this.durationMillis = durationMillis;
        this.category = category;
        SleepWork.debug = debug;
        setProgress(Progress.PROGRESS_0_PC);
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
        try {
            doWork();
        } catch (InterruptedException e) {
            // restore interrupted status
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    protected void doWork() throws InterruptedException {
        if (debug) {
            setStatus("Starting sleep work");
            readyLatch.countDown();
            startLatch.await();
            setStatus("Running sleep work");
        }

        for (;;) {
            long elapsed = System.currentTimeMillis() - getStartTime();
            if (elapsed > durationMillis) {
                break;
            }
            setProgress(new Progress(100F * elapsed / durationMillis));

            if (isSuspending()) {
                durationMillis -= elapsed; // save state
                suspended();
                if (debug) {
                    doneLatch.countDown();
                    finishLatch.await();
                }
                return;
            }

            Thread.sleep(10);
        }

        if (debug) {
            setStatus("Completed sleep work");
            setProgress(Progress.PROGRESS_100_PC);
            doneLatch.countDown();
            finishLatch.await();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + (getId().length() > 10 ? "" : (getId() + ", ")) + durationMillis
                + "ms, " + getProgress() + ")";
    }

    public void debugWaitReady() throws InterruptedException {
        readyLatch.await();
    }

    public void debugWaitDone() throws InterruptedException {
        doneLatch.await();
    }

    public void debugStart() {
        startLatch.countDown();
    }

    public void debugFinish() {
        finishLatch.countDown();
    }

}
