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
 *     matic
 */
package org.nuxeo.ecm.platform.video.service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.nuxeo.ecm.core.event.impl.AsyncEventExecutor;

/**
 * @author matic
 *
 */
public class VideoConversionExecutor {

    protected final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();

    protected final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 5, TimeUnit.MINUTES,
                queue, new AsyncEventExecutor.NamedThreadFactory("Nuxeo Async Video Conversion"));

    public void execute(VideoConversionTask task) {
        executor.execute(task);
    }

    public boolean shutdownNow() {

        executor.shutdownNow();

        return waitForCompletions(2);
    }

    public boolean shutdown()  {

        executor.shutdown();

        return waitForCompletions(300);
    }

    public boolean waitForCompletions(long timeout) {
        try {
            if (!executor.awaitTermination(timeout, TimeUnit.SECONDS)) {
                return false;
            }
        } catch (InterruptedException e) {
            return false;
        }
        return true;
    }

    public static VideoConversionExecutor create() {
        return new VideoConversionExecutor();
    }

}
