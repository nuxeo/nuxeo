package org.nuxeo.ecm.platform.importer.queue.manager;/*
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
 *     bdelbosc
 */

import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Blocking Queues Manager, in memory queues
 * @since 8.10
 */
public class BQManager extends AbstractQueuesManager {

    final List<BlockingQueue<SourceNode>> queues;

    protected final int maxQueueSize;

    public BQManager(ImporterLogger logger, int queuesNb, int maxQueueSize) {
        super(logger, queuesNb);
        this.maxQueueSize = maxQueueSize;
        queues = new ArrayList<>(queuesNb);
        for (int i = 0; i < queuesNb; i++) {
            queues.add(new ArrayBlockingQueue<>(maxQueueSize));
        }
    }

    @Override
    public boolean isEmpty(int queue) {
        return queues.get(queue).isEmpty();
    }

    @Override
    public SourceNode poll(int queue, long timeout, TimeUnit unit) throws InterruptedException {
        return queues.get(queue).poll(timeout, unit);
    }

    @Override
    public int size(int queue) {
        return queues.get(queue).size();
    }

    @Override
    public SourceNode poll(int queue) {
        return queues.get(queue).poll();
    }

    @Override
    public void put(int queue, SourceNode node) throws InterruptedException {
        queues.get(queue).put(node);
    }

}
