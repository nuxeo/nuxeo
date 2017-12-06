/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     tiry
 */
package org.nuxeo.ecm.core.event.pipe;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.pipe.local.LocalEventBundlePipeConsumer;
import org.nuxeo.runtime.api.Framework;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Simple Queue based implementation that starts a dedicated thread to consume an in-memory message queue.
 *
 * @since 8.4
 */
public class QueueBaseEventBundlePipe extends AbstractEventBundlePipe<EventBundle> {

    protected static Log log = LogFactory.getLog(QueueBaseEventBundlePipe.class);

    protected ConcurrentLinkedQueue<EventBundle> queue;

    protected ThreadPoolExecutor consumerTPE;

    protected LocalEventBundlePipeConsumer consumer;

    protected boolean stop;

    protected int batchSize = 10;

    @Override
    public void initPipe(String name, Map<String, String> params) {
        super.initPipe(name, params);
        stop = false;

        if (params.containsKey("batchSize")) {
            try {
                batchSize = Integer.parseInt(params.get("batchSize"));
            } catch (NumberFormatException e) {
                log.error("Unable to read batchSize parameter", e);
            }
        }
        queue = new ConcurrentLinkedQueue<>();
        consumerTPE = new ThreadPoolExecutor(1, 1, 60, TimeUnit.MINUTES, new LinkedBlockingQueue<>());
        consumerTPE.prestartCoreThread();
        consumerTPE.execute(new Runnable() {

            private boolean send(List<EventBundle> messages) {
                if (consumer.receiveMessage(messages)) {
                    messages.clear();
                    return true;
                }

                // keep the events that can not be processed ?
                queue.addAll(messages);
                return false;
            }

            @Override
            public void run() {

                consumer = new LocalEventBundlePipeConsumer();
                consumer.initConsumer(getName(), getParameters());
                boolean interrupted = false;
                try {
                    while (!stop) {
                        List<EventBundle> messages = new ArrayList<>();
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

                        // XXX this is a hack ! TODO: find a better approach
                        try {
                            if (Framework.isTestModeSet()) {
                                Thread.sleep(5);
                            } else {
                                Thread.sleep(200);
                            }
                        } catch (InterruptedException e) {
                            interrupted = true;
                        }
                    }
                } finally {
                    if (interrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
        consumerTPE.shutdown();
    }

    @Override
    protected EventBundle marshall(EventBundle events) {
        return events;
    }

    @Override
    protected void send(EventBundle message) {
        queue.offer(message);
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
        // XXX use Condition
        try {
            do {
                if (queue.size() == 0) {
                    return true;
                }
                Thread.sleep(pause);
            } while (System.currentTimeMillis() < deadline);
        } finally {
            if (consumerTPE != null) {
                consumerTPE.awaitTermination(pause, TimeUnit.MILLISECONDS);
            }
        }

        return false;
    }

}
