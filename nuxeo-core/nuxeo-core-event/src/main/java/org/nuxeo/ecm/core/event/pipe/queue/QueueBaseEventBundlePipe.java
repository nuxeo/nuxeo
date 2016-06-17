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

import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.pipe.AbstractEventBundlePipe;
import org.nuxeo.ecm.core.event.pipe.local.LocalEventBundlePipeConsumer;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * @since TODO
 */
public class QueueBaseEventBundlePipe extends AbstractEventBundlePipe<EventBundle> {


    protected ConcurrentLinkedQueue<EventBundle> queue;

    protected ThreadPoolExecutor consumerTPE;

    @Override
    public void initPipe(String name, Map<String, String> params) {
        super.initPipe(name, params);
        queue = new ConcurrentLinkedQueue<EventBundle>();
        consumerTPE = new ThreadPoolExecutor(1, 1, 60, TimeUnit.MINUTES,  new LinkedBlockingQueue<Runnable>());
        consumerTPE.prestartCoreThread();
        consumerTPE.execute(new Runnable() {

            @Override
            public void run() {

                LocalEventBundlePipeConsumer consumer = new LocalEventBundlePipeConsumer();
                consumer.initConsumer(getName(), getParameters());

                while(true) {
                    List<EventBundle> messages = new ArrayList<EventBundle>();
                    EventBundle message;
                    while ((message = queue.poll()) !=null) {
                        messages.add(message);
                        if (messages.size() > 9) {
                            consumer.receiveMessage(messages);
                            messages.clear();
                        }
                    }
                    if (messages.size() > 0) {
                        consumer.receiveMessage(messages);
                        messages.clear();
                    }
                    try {
                        if (Framework.isTestModeSet()) {
                            // I know this is a hack !
                            Thread.sleep(5);
                        } else {
                            Thread.sleep(500);
                        }
                    } catch (InterruptedException e) {
                        consumerTPE.shutdown();
                    }
                }
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
    public boolean waitForCompletion(long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        int pause = (int) Math.min(timeoutMillis, 500L);
        do {
            if (queue.size()==0) {
                return true;
            }
            Thread.sleep(pause);
        } while (System.currentTimeMillis() < deadline);
        return false;
    }


}
