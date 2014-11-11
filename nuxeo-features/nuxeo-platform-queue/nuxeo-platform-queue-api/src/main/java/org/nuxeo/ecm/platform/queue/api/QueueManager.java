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
 * Contributors: Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
package org.nuxeo.ecm.platform.queue.api;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Handle contents that needs long processing such as OCRing a document, images
 * manipulation, and eventually sequencing.
 *
 * Handle only content of same type. Delegate the content saving to a dedicated
 * persister .
 *
 * @author Stephane Lacoin <slacoin@nuxeo.com> (aka matic)
 * @see QueuePersister
 *
 */
public interface QueueManager {

    /**
     * List infos about content being handled.
     *
     * @return the list
     */
    List<QueueItem> listHandledItems();

    /**
     * List infos about content waiting for recovery.
     *
     * @return the list
     */
    public List<QueueItem> listOrphanedItems();

    /**
     * Check for the existence of an atomic content on persistence back-end.
     * 
     * @param content the content
     * @return true if content is already present on persistence back-end
     * @throws QueueException
     */
    public boolean knowsContent(QueueContent content) throws QueueException;

    /**
     * Remove content from persistence back-end.
     *
     * @param content the content
     */
    public void forgetContent(QueueContent content);

    /**
     * Update additional item informations on persistence back-end.
     *
     * @param content the content
     * @param additionalInfos the additional infos
     */
    public void updateItem(QueueContent content,
            Map<String, Serializable> additionalInfos);

}
