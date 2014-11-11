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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.queue.api.QueueContent;
import org.nuxeo.ecm.platform.queue.api.QueueException;
import org.nuxeo.ecm.platform.queue.api.QueueItem;
import org.nuxeo.ecm.platform.queue.api.QueueManager;
import org.nuxeo.ecm.platform.queue.api.QueuePersister;

/**
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * 
 */
public class QueueManagerImpl implements QueueManager {

    QueuePersister persister;

    String queueName;

    public QueueManagerImpl(QueuePersister persister, String queueName) {
        this.persister = persister;
        this.queueName = queueName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.nuxeo.ecm.platform.queue.api.QueueManager#forgetContent(org.nuxeo
     * .ecm.platform.queue.api.QueueContent)
     */
    public void forgetContent(QueueContent content) {
        persister.forgetContent(content);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.nuxeo.ecm.platform.queue.api.QueueManager#knowsContent(org.nuxeo.
     * ecm.platform.queue.api.QueueContent)
     */
    public boolean knowsContent(QueueContent content) throws QueueException {
        return persister.hasContent(content);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.nuxeo.ecm.platform.queue.api.QueueManager#listHandledItems()
     */
    public List<QueueItem> listHandledItems() {
        ArrayList<QueueItem> handledItems = new ArrayList<QueueItem>();
        for (QueueItem item : persister.listKnownItems(queueName)) {
            handledItems.add(item);
        }
        return handledItems;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.nuxeo.ecm.platform.queue.api.QueueManager#listOrphanedItems()
     */
    public List<QueueItem> listOrphanedItems() {
        ArrayList<QueueItem> orphansList = new ArrayList<QueueItem>();
        for (QueueItem item : persister.listKnownItems(queueName)) {
            if (item.isOrphaned()) {
                orphansList.add(item);
            }
        }
        return orphansList;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.nuxeo.ecm.platform.queue.api.QueueManager#updateItem(org.nuxeo.ecm
     * .platform.queue.api.QueueContent, java.util.Map)
     */
    public void updateItem(QueueContent content,
            Map<String, Serializable> additionalInfos) {

    }

}
