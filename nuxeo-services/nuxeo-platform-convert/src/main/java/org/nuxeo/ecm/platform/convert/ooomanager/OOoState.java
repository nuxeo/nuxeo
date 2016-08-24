/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.platform.convert.ooomanager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author bogdan
 * @since 9.2
 */
public class OOoState {

    public static final int STOPPED = 0;

    public static final int STARTING = 1;

    public static final int STARTED = 2;

    public static final int STOPPING = 3;

    protected volatile int status = STOPPED;

    protected volatile CountDownLatch startLatch = null;

    public synchronized void update(int state) {
        if (state == STARTING) {
            this.startLatch = new CountDownLatch(1);
            this.status = STARTING;
        } else {
            this.status = state;
            // release latch (usually state is STARTED)
            if (startLatch != null) {
                startLatch.countDown(); // release it if not released
                startLatch = null;
            }
        }
    }

    public int getStatus() {
        return status;
    }

    public boolean isStarted() {
        return status == STARTED;
    }

    public boolean isStarting() {
        return status == STARTING;
    }

    public boolean isStopping() {
        return status == STOPPING;
    }

    public boolean isStopped() {
        return status == STOPPED;
    }

    /**
     * Same as {@link #isAlive(long, TimeUnit)} with a 60 seconds timeout
     */
    public boolean isAlive() throws InterruptedException {
        return isAlive(60, TimeUnit.SECONDS);
    }

    /**
     * Tests whether the server is started. If the servers is in starting state then waits until it is fully started If
     * the server is already started returns true. If it is stopped or stopping returns false. If it is starting then
     * waits using the given timeout until either the server is started or timeout is reached. Returns true if at the
     * end of the method the server is started, false otherwise
     */
    public boolean isAlive(long timeout, TimeUnit unit) throws InterruptedException {
        CountDownLatch latch = startLatch;
        if (latch != null) {
            latch.await(timeout, unit);
        }
        return status == STARTED;
    }

}
