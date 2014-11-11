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

import java.util.Date;
import java.util.List;

/**
 * 
 * Save contents on a persistent back-end. Three implementation are available by
 * default. The first one, will implement the persister in memory for testing
 * only. The second one, will be will be based on Nuxeo and the last one on file
 * system. *
 * 
 * @author Stephane Lacoin <slacoin@nuxeo.com> (aka matic)
 * 
 */
public interface QueuePersister {

    /**
     * Save content on persistent back-end.
     * 
     * @param content the content
     * @return the atomic item
     */
    QueueItem saveContent(QueueContent content) throws QueueException;

    /**
     * Remove content from the persistent back-end.
     * 
     * @param item the item
     * @throws QueueException
     */
    void forgetContent(QueueContent content);

    /**
     * Update item attributes on persistent back-end.
     * 
     * @param item the item
     */
    void updateItem(QueueItem item);

    /**
     * List known contents.
     * 
     * @param queueName
     * @return
     */
    List<QueueItem> listKnownItems(String queueName);

    /**
     * Does the persister knows this content ?
     * 
     * @param content
     * @return
     */
    boolean hasContent(QueueContent content) throws QueueException;

    /**
     * Should be manually be called by the handler when an executor is launched.
     * 
     * @param content
     * @param date
     */
    void setExecuteTime(QueueContent content, Date date);

}
