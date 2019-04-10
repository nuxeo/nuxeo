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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.nuxeo.ecm.platform.importer.source.SourceNode;

/**
 * @since 8.3
 */
public interface QueuesManager {

    /**
     * Returns the number of queues
     */
    int count();

    /**
     * Put a node into a queue
     *
     * @throws InterruptedException
     */
    void put(int queue, SourceNode node) throws InterruptedException;

    /**
     * Get a node from a queue.
     *
     */
    SourceNode poll(int queue);

    /**
     * Get a node from a queue, with a timeout.
     *
     * @throws InterruptedException
     */
    SourceNode poll(int queue, long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Returns true if there is no element in the queue.
     */
    boolean isEmpty(int queue);

    /**
     * Returns the number of elements in the queue.
     */
    int size(int queue);

    /**
     * Dispatch the node to a queue
     * @param node
     * @return the queue number
     * @throws InterruptedException
     */
    @Deprecated
    int dispatch(SourceNode node) throws InterruptedException;

    /**
     * use getQueueCount instead
     * @return
     */
    @Deprecated
    int getNBConsumers();

    @Deprecated
    BlockingQueue<SourceNode> getQueue(int idx);
}