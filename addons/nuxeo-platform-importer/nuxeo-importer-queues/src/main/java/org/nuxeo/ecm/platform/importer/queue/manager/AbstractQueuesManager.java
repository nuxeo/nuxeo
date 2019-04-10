/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.importer.queue.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

/**
 * @since 8.3
 */
public abstract class AbstractQueuesManager implements QueuesManager {

    final List<BlockingQueue<SourceNode>> queues;

    protected final int maxQueueSize;

    protected final ImporterLogger log;

    public AbstractQueuesManager(ImporterLogger logger, int queuesNb, int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
        queues = new ArrayList<BlockingQueue<SourceNode>>(queuesNb);
        for (int i = 0; i < queuesNb; i++) {
            queues.add(new ArrayBlockingQueue<>(maxQueueSize));
        }
        log = logger;
    }

    @Override
    public BlockingQueue<SourceNode> getQueue(int idx) {
        return queues.get(idx);
    }

    @Override
    public boolean isQueueEmpty(int idQueue) {
        return queues.get(idQueue).isEmpty();
    }

    @Override
    public int dispatch(SourceNode bh) throws InterruptedException {
        int idx = getTargetQueue(bh, queues.size());
        getQueue(idx).put(bh);
        return idx;
    }

    protected abstract int getTargetQueue(SourceNode bh, int nbQueues);

    @Override
    public int getNBConsumers() {
        return queues.size();
    }
}
