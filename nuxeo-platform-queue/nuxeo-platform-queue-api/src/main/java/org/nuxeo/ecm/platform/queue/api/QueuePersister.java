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
 * Contributors: Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>, Stephane Lacoin <slacoin@nuxeo.com> (aka matic)
 */
package org.nuxeo.ecm.platform.queue.api;

import java.io.Serializable;
import java.net.URI;
import java.util.Date;
import java.util.List;


/**
 * Save contents on a persistent back-end. Three implementation are available by
 * default. The first one, will implement the persister in memory for testing
 * only. The second one, will be will be based on Nuxeo and the last one on file
 * system. *
 *
 * @author Stephane Lacoin <slacoin@nuxeo.com> (aka matic)
 *
 */
public interface QueuePersister<C extends Serializable> {

    /**
     * Create content on persistent back-end.
     *
     * @param name the content name
     * @param owner the owner name
     * @param content the handled content
     * @return the atomic item
     */
    QueueInfo<C> addContent(URI name, URI owner, C content);

    /**
     * Retrieves informations about an handled content in back-end
     *
     * @param type the content type
     * @return name the content name
     */
    QueueInfo<C> getInfo(URI name);

    /**
     * Remove content from the persistent back-end.
     *
     * @param name the content name
     */
    QueueInfo<C> removeContent(URI name);

    /**
     * Update additional infos on persistent back-end.
     *
     * @param name the content name
     */
    void updateContent(URI name, C content);

    /**
     * List known contents.
     *
     * @return the informations about known contents
     */
    List<QueueInfo<C>> listKnownItems();

    /**
     * Does the persister knows this content ?
     *
     * @param the content name
     * @return
     */
    boolean hasContent(URI name);

    /**
     * Should be manually be called by the handler when a content processing is launched.
     *
     * @param the content name
     */
    QueueInfo<C> setLaunched(URI name);


    /**
     * Should be manually be called by the handler when a task is finished
     *
     * @param the content name
     */
    QueueInfo<C> setBlacklisted(URI contentName);

    /**
     * List contents that matches the content owner name
     *
     * @param queueContent
     */
    List<QueueInfo<C>> listByOwner(URI name);

    /**
     * Remove contents that matches the content owner name
     *
     * @param the owner name
     * @return the number of contents removed
     */
    int removeByOwner(URI name);

    /**
     * Remote out-dated black-listed content from a specified date
     *
     * @param the date from where the content should be removed
     * @return the number of content removed
     */
    int removeBlacklisted(Date from);

    /**
     * Create the queue if it does not exist already
     *
     * @return
     */
    void createIfNotExist();



}
