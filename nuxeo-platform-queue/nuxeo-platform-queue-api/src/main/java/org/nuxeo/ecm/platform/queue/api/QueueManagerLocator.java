/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.queue.api;

import java.util.List;

/*
 *
 */

// TODO: Auto-generated Javadoc
/**
 * Provide access to atomic objects and services.
 *
 * @author "Stephane Lacoin at Nuxeo (aka matic)"
 */
public interface QueueManagerLocator {

    /**
     * Provide an access to the atomic queue that is in charge the provided
     * content.
     *
     * @param content the content
     * @return queue
     */
    QueueManager locateQueue(QueueContent content)
            throws QueueNotFoundException;

    /**
     * Provide an access to the atomic queue.
     *
     * @param type the type
     * @return queue
     */
    QueueManager locateQueue(String queueName) throws QueueNotFoundException;

    /**
     * Provide the list of registered queues
     *
     * @return
     */
    List<String> getAvailableQueues();

}
