/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
package org.nuxeo.ecm.platform.queue.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.queue.api.QueueContent;
import org.nuxeo.ecm.platform.queue.api.QueueExecutor;
import org.nuxeo.ecm.platform.queue.api.QueueFactory;
import org.nuxeo.ecm.platform.queue.api.QueueNotFoundException;
import org.nuxeo.ecm.platform.queue.api.QueuePersister;

/**
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * 
 */
public class QueueFactoryImpl implements QueueFactory {

    Map<String, QueueRegistryEntry> queueRegistry = new HashMap<String, QueueRegistryEntry>();

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.nuxeo.ecm.platform.queue.api.QueueFactory#createQueue(java.net.URI,
     * org.nuxeo.ecm.platform.queue.api.AtomicPersister,
     * org.nuxeo.ecm.platform.queue.api.AtomicExecutor)
     */
    public void createQueue(String name, QueuePersister persister,
            QueueExecutor executor) {
        queueRegistry.put(name, new QueueRegistryEntry(executor, persister));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.nuxeo.ecm.platform.queue.api.QueueFactory#getPersister(org.nuxeo.
     * ecm.platform.queue.api.AtomicContent)
     */
    public QueuePersister getPersister(QueueContent content)
            throws QueueNotFoundException {
        return getPersister(content.getDestination());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.nuxeo.ecm.platform.queue.api.QueueFactory#getExecutor(org.nuxeo.ecm
     * .platform.queue.api.AtomicContent)
     */
    public QueueExecutor getExecutor(QueueContent content)
            throws QueueNotFoundException {
        return getExecutor(content.getDestination());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.nuxeo.ecm.platform.queue.api.QueueFactory#getExecutor(java.lang.String
     * )
     */
    public QueueExecutor getExecutor(String queueName)
            throws QueueNotFoundException {
        QueueRegistryEntry entry = queueRegistry.get(queueName);
        if (entry == null || entry.getExecutor() == null) {
            throw new QueueNotFoundException("Executor is not available "
                    + queueName);
        }
        return entry.getExecutor();

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.nuxeo.ecm.platform.queue.api.QueueFactory#getPersister(java.lang.
     * String)
     */
    public QueuePersister getPersister(String queueName)
            throws QueueNotFoundException {
        QueueRegistryEntry entry = queueRegistry.get(queueName);
        if (entry == null || entry.getPersister() == null) {
            throw new QueueNotFoundException("Persister is not available "
                    + queueName);
        }
        return entry.getPersister();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.nuxeo.ecm.platform.queue.api.QueueFactory#getRegisteredQueues()
     */
    public List<String> getRegisteredQueues() {
        return new ArrayList<String>(queueRegistry.keySet());
    }
}
