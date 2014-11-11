/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
    protected transient boolean debug;

    /** used for debug. */
    protected transient CountDownLatch readyLatch = new CountDownLatch(1);

    /** used for debug. */
    protected transient CountDownLatch doneLatch = new CountDownLatch(1);

    /** used for debug. */
    protected transient CountDownLatch startLatch = new CountDownLatch(1);

    /** used for debug. */
    protected transient CountDownLatch finishLatch = new CountDownLatch(1);

    /**
     * Creates a work instance that does nothing but sleep.
     *
     * @param durationMillis the sleep duration
     */
    public SleepWork(long durationMillis) {
        this(durationMillis, "SleepWork", false);
    }

    /**
     * If debug is true, then the various debug* methods must be called in the
     * proper order for the work to start and stop: {@link #debugStart},
     * {@link #debugFinish}.
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
        this.debug = debug;
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
    public void work() throws InterruptedException {
        if (debug) {
            setStatus("Starting sleep work");
            readyLatch.countDown();
            startLatch.await();
            setStatus("Running sleep work");
        }

        startTime = System.currentTimeMillis();
        for (;;) {
            long elapsed = System.currentTimeMillis() - startTime;
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
        return getClass().getSimpleName() + "("
                + (getId().length() > 10 ? "" : (getId() + ", "))
                + durationMillis + "ms, " + getProgress() + ")";
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
