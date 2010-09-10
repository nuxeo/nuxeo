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

import java.io.Serializable;
import java.net.URI;
import java.util.List;

/**
 * Register persisters and processors giving an handled context class
 *
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * @author "Stephane Lacoin at Nuxeo (aka matic)"
 */
public interface QueueRegistry {

    /**
     * Registers a content persister and processor giving their content class
     *
     * @param queueName the queue name
     * @param contentType the content type
     * @param queuePersister the persister
     * @param queueProcessor the processor
     */
    <C extends Serializable> void register(String queueName, Class<C> contentType, QueuePersister<C> persister, QueueProcessor<C> processor);

    /**
     * Returns the  persister associated to  a content type.
     *
     * @param content
     * @return
     */
    <C extends Serializable> QueuePersister<C> getPersister(URI name);

    /**
     *  Returns the processor associated to a content type
     */
    <C extends Serializable> QueueProcessor<C> getProcessor(URI name);

    /**
     * List the registered queues.
     */
    List<URI> getQueueNames();

}
