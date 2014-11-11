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
 * Contributors:    Stephane Lacoin at Nuxeo (aka matic),
 *                  Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
package org.nuxeo.ecm.platform.queue.api;

import java.io.Serializable;
import java.net.URI;

/**
 * Handle content into dedicated queues and handle
 *
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * @author "Stephane Lacoin at Nuxeo (aka matic)"
 */
public interface QueueHandler {

    /**
     * Inject and process new content in queue
     *
     * @param content the content
     */
    @Transacted
    <C extends Serializable> void newContent(URI owner, URI contentName,  C content);

    /**
     * Register and process content if unknown.
     *
     * @param ownerName the context owner
     * @param resource the content name
     */
    @Transacted
    <C extends Serializable> void newContentIfUnknown(URI ownerName, URI contentName,  C content);

    /**
     * Generate a name referencing an unique content
     *
     * @param queueName
     * @param contentName
     * @return
     */
    URI newName(String queueName, String contentName);

    /**
     * Cancel content processing
     *
     * @param queueName
     * @param contentName
     * @return
     */
    @Transacted
    <C extends Serializable> QueueInfo<C> blacklist(URI contentName);

    /**
     * Retry content processing
     *
     * @param contentName
     * @return
     */
    @Transacted
    <C extends Serializable> QueueInfo<C> retry(URI contentName);
}
