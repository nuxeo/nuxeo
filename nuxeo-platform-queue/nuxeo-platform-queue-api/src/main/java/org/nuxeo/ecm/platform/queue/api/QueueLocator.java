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

import java.io.Serializable;
import java.net.URI;
import java.util.List;

/**
 * Provide access to queue services.
 *
 * @author "Stephane Lacoin at Nuxeo (aka matic)"
 */
public interface QueueLocator {

    /**
     * Provide an access to a queue manager giving the handled context class
     *
     * @param type the context class handled
     * @return the manager
     */
    <C extends Serializable> QueueManager<C> getManager(URI name);

    /**
     * Provides the list of managers of any handled context class.
     *
     * @return the list of queue managers
     */
    List<QueueManager<?>> getManagers();

    /**
     * Build a queue URI
     *
     * @param name the queue name
     * @return the queue URI
     */
    URI newQueueName(String name);

    /**
     * Build a content name URI
     *
     * @param queueName the queue name
     * @param contentName the content name
     * @return the content URI
     */
    URI newContentName(String queueName, String contentName);

    /**
     * Build a content name URI
     *
     * @param name the base name
     * @param contentName the content name
     * @return the content URI
     */
    URI newContentName(URI name, String contentName);

}
