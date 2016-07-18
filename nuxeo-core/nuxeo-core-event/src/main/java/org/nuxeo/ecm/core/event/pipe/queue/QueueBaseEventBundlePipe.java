/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     tiry
 */
package org.nuxeo.ecm.core.event.pipe.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.annotation.Experimental;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.pipe.AbstractEventBundlePipe;
import org.nuxeo.ecm.core.event.pipe.local.LocalEventBundlePipeConsumer;
import org.nuxeo.runtime.api.Framework;

/**
 * Simple Queue based implementation that starts a dedicated thread to consume an in-memory message queue.
 *
 * @since 8.4
 */
@Experimental
public class QueueBaseEventBundlePipe extends AbstractEventBundlePipe<EventBundle> {

    protected static Log log = LogFactory.getLog(QueueBaseEventBundlePipe.class);

    protected ConcurrentLinkedQueue<EventBundle> queue;

    protected ThreadPoolExecutor consumerTPE;

    protected LocalEventBundlePipeConsumer consumer;

    protected boolean stop = false;

    protected int batchSize = 10;

    @Override
    public void initPipe(String name, Map<String, String> params) {
        super.initPipe(name, params);
        stop = false;

        if (params.containsKey("batchSize")) {
            try {
                batchSize = Integer.parseInt(params.get(batchSize));
            } catch (NumberFormatException e) {
                log.error("Unable to read batchSize parameter", e);
            }
        }

        queue = new ConcurrentLinkedQueue<EventBundle>();
        consumerTPE = new ThreadPoolExecutor(1, 1, 60, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());
        consumerTPE.prestartCoreThread();
        consumerTPE.execute(new Runnable() {

            protected boolean send(List<EventBundle> messages) {
                if (consumer.receiveMessage(messages)) {
                    messages.clear();
                } else {
                    // keep the events that can not be processed ?
                    queue.addAll(messages);
                    return false;
                }
                return true;
            }

            @Override
            public void run() {

                consumer = new LocalEventBundlePipeConsumer();
                consumer.initConsumer(getName(), getParameters());
                while (!stop) {
                    List<EventBundle> messages = new ArrayList<EventBundle>();
                    EventBundle message;
                    while ((message = queue.poll()) != null) {
                        messages.add(message);
                        if (messages.size() >= batchSize) {
                            send(messages);
                        }
                    }
                    if (messages.size() > 0) {
                        send(messages);
                    }

                    // XXX this is a hack !
                    try {
                        if (Framework.isTestModeSet()) {
                            Thread.sleep(5);
                        } else {
                            Thread.sleep(200);
                        }
                    } catch (InterruptedException e) {
                        consumerTPE.shutdown();
                    }
                }
                consumer = null;
            }
        });
    }

    @Override
    protected EventBundle marshall(EventBundle events) {
        return events;
    }

    @Override
    protected void send(EventBundle message) {
        queue.add(message);
    }

    @Override
    public void shutdown() throws InterruptedException {
        stop = true;
        // XXX
        waitForCompletion(5000L);
        if (consumer != null) {
            consumer.shutdown();
        }
    }

    @Override
    public boolean waitForCompletion(long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        int pause = (int) Math.min(timeoutMillis, 500L);
        do {
            if (queue.size() == 0) {
                return true;
            }
            Thread.sleep(pause);
        } while (System.currentTimeMillis() < deadline);
        return false;
    }

}
