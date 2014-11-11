/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License (LGPL)
 * version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * Contributors: Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>, Stephane Lacoin at Nuxeo (aka matic)
 */
package org.nuxeo.ecm.platform.queue.api;

import java.util.List;

// FIXME: not clear.
/**
 * Create atomic objects.
 *
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * @author "Stephane Lacoin at Nuxeo (aka matic)"
 */
public interface QueueFactory {

    // FIXME: this comment is not consistent with the method signature
    /**
     * Creates and registers a new queue given a content class, an atomic processor
     * and a dedicated content persister.
     *
     * @param contentClass the content class
     * @param persister the persister
     */
    void createQueue(String name, QueuePersister persister,
            QueueExecutor executor);

    // FIXME: for a queue or for a queue content?
    /**
     * Returns a persister for a queue.
     *
     * @param content
     * @return
     * @throws QueueException
     */
    QueuePersister getPersister(QueueContent content)
            throws QueueNotFoundException;

    /**
     * Return a persister for a queue.
     */
    QueuePersister getPersister(String queueName) throws QueueNotFoundException;

    // FIXME: for a queue or for a queue content?
    /**
     * Gets an executor for a queue
     */
    QueueExecutor getExecutor(QueueContent content)
            throws QueueNotFoundException;

    /**
     * Gets an executor for a queue.
     */
    QueueExecutor getExecutor(String queueName) throws QueueNotFoundException;

    /**
     * Returns the list of registered queues.
     */
    List<String> getRegisteredQueues();

}
