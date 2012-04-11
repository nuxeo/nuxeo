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
 * Simple dummy work class for unit tests.
 */
public class DummyWork extends AbstractWork {

    protected static final int TIME_MILLIS = 5000; // 5s

    protected CountDownLatch readyLatch = new CountDownLatch(1);

    protected CountDownLatch doneLatch = new CountDownLatch(1);

    protected CountDownLatch startLatch = new CountDownLatch(1);

    protected CountDownLatch finishLatch = new CountDownLatch(1);

    public DummyWork() {
        setStatus("Dummy work");
    }

    @Override
    public String getCategory() {
        return "dummy";
    }

    @Override
    public void work() throws InterruptedException {
        setStatus("Starting dummy work");

        readyLatch.countDown();
        startLatch.await();

        startTime = System.currentTimeMillis();

        while (true) {
            long current = System.currentTimeMillis();
            if (current > startTime + TIME_MILLIS) {
                break;
            }
            float pc = 100F * (current - startTime) / TIME_MILLIS;
            if (pc > 100) {
                pc = 100F;
            }
            setStatus("In progress dummy work");
            setProgress(new Progress(pc));

            Thread.sleep(10);
        }

        setStatus("Finished dummy work");
        setProgress(Progress.PROGRESS_100_PC);
        doneLatch.countDown();
        finishLatch.await();
    }

    protected void debugWaitReady() throws InterruptedException {
        readyLatch.await();
    }

    protected void debugWaitDone() throws InterruptedException {
        doneLatch.await();
    }

    protected void debugStart() {
        startLatch.countDown();
    }

    protected void debugFinish() {
        finishLatch.countDown();
    }

}
