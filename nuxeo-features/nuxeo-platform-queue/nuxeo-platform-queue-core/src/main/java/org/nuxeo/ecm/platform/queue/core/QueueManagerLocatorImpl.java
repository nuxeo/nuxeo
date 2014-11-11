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

import java.util.List;

import org.nuxeo.ecm.platform.queue.api.QueueContent;
import org.nuxeo.ecm.platform.queue.api.QueueFactory;
import org.nuxeo.ecm.platform.queue.api.QueueManager;
import org.nuxeo.ecm.platform.queue.api.QueueManagerLocator;
import org.nuxeo.ecm.platform.queue.api.QueueNotFoundException;
import org.nuxeo.ecm.platform.queue.api.QueuePersister;
import org.nuxeo.runtime.api.Framework;

/**
 * Implementation of the queue manager locator.
 * 
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * 
 */
public class QueueManagerLocatorImpl implements QueueManagerLocator {

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.nuxeo.ecm.platform.queue.api.QueueManagerLocator#locateQueue(org.
     * nuxeo.ecm.platform.queue.api.QueueContent)
     */
    public QueueManager locateQueue(QueueContent content)
            throws QueueNotFoundException {
        return locateQueue(content.getDestination());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.nuxeo.ecm.platform.queue.api.QueueManagerLocator#locateQueue(java
     * .lang.String)
     */
    public QueueManager locateQueue(String queueName)
            throws QueueNotFoundException {
        QueueFactory factory = Framework.getLocalService(QueueFactory.class);
        QueuePersister persister = factory.getPersister(queueName);
        return new QueueManagerImpl(persister, queueName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.nuxeo.ecm.platform.queue.api.QueueManagerLocator#getRegisteredQueues
     * ()
     */
    public List<String> getAvailableQueues() {
        // the locator may return more information than just the list of queue
        // name
        QueueFactory factory = Framework.getLocalService(QueueFactory.class);
        return factory.getRegisteredQueues();
    }

}
