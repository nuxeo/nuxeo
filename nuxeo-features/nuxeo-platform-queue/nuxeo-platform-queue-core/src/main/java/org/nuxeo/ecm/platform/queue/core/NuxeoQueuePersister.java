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
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.heartbeat.api.ServerHeartBeat;
import org.nuxeo.ecm.platform.queue.api.QueueContent;
import org.nuxeo.ecm.platform.queue.api.QueueException;
import org.nuxeo.ecm.platform.queue.api.QueueItem;
import org.nuxeo.ecm.platform.queue.api.QueuePersister;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 *
 */
public class NuxeoQueuePersister implements QueuePersister, NuxeoQueueConstants {


    public static final Log log = LogFactory.getLog(NuxeoQueuePersister.class);

    /*
     * (non-Javadoc)
     *
     * @see
     * org.nuxeo.ecm.platform.queue.api.AtomicPersister#forgetContent(org.nuxeo
     * .ecm.platform.queue.api.AtomicContent)
     */
    public void forgetContent(QueueContent content) {

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.nuxeo.ecm.platform.queue.api.AtomicPersister#hasContent(org.nuxeo
     * .ecm.platform.queue.api.AtomicContent)
     */
    public boolean hasContent(QueueContent content) throws QueueException {
        try {
            String defaultRepositoryName = Framework.getLocalService(
                    RepositoryManager.class).getDefaultRepository().getName();
            HasContentRunner runner = new HasContentRunner(
                    defaultRepositoryName, content);
            runner.runUnrestricted();
            return runner.hasContent;
        } catch (ClientException e) {
            throw new QueueException(
                    "A problem occured while trying to save the content", e);
        }

    }

    class HasContentRunner extends UnrestrictedSessionRunner {

        boolean hasContent = false;

        QueueContent content;

        public HasContentRunner(String repository, QueueContent content) {
            super(repository);
            this.content = content;
        }

        @Override
        public void run() throws ClientException {
            hasContent = session.exists(new PathRef("/" + QUEUE_ROOT_NAME + "/"
                    + content.getDestination() + "/" + content.getName()));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.nuxeo.ecm.platform.queue.api.AtomicPersister#listKnownItems()
     */
    public List<QueueItem> listKnownItems(String queueName) {
        try {
            ListKnownItem runner = new ListKnownItem(Framework.getLocalService(
                    RepositoryManager.class).getDefaultRepository().getName(),
                    queueName);
            runner.runUnrestricted();
            ArrayList<QueueItem> queueItemList = new ArrayList<QueueItem>(
                    runner.doclist.size());
            for (DocumentModel doc : runner.doclist) {
                queueItemList.add(doc.getAdapter(QueueItem.class));
            }
            return queueItemList;
        } catch (ClientException e) {
            throw new Error("Couldn't get the list queue item for the queue: "
                    + queueName, e);
        }
    }

    class ListKnownItem extends UnrestrictedSessionRunner {

        boolean hasContent = false;

        String queueName;

        DocumentModelList doclist;

        public ListKnownItem(String repository, String queueName) {
            super(repository);
            this.queueName = queueName;
        }

        @Override
        public void run() throws ClientException {
            DocumentModel queueDoc = getOrCreateQueue(session, queueName);
            doclist = session.getChildren(queueDoc.getRef());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.nuxeo.ecm.platform.queue.api.AtomicPersister#saveContent(org.nuxeo
     * .ecm.platform.queue.api.AtomicContent)
     */
    public QueueItem saveContent(QueueContent content) throws QueueException {

        try {
            String defaultRepositoryName = Framework.getLocalService(
                    RepositoryManager.class).getDefaultRepository().getName();
            new SaveContentRunner(defaultRepositoryName, content).runUnrestricted();
        } catch (ClientException e) {
            throw new QueueException(
                    "A problem occured while trying to save the content", e);
        }
        return null;
    }

    class SaveContentRunner extends UnrestrictedSessionRunner {
        QueueContent content;

        public SaveContentRunner(String repository, QueueContent content) {
            super(repository);
            this.content = content;
        }

        @Override
        public void run() throws ClientException {

            ServerHeartBeat heartbeat = Framework.getLocalService(ServerHeartBeat.class);
            DocumentModel queueFolder = getOrCreateQueue(session,
                    content.getDestination());
            DocumentRef queueItemRef = new PathRef(
                    queueFolder.getPathAsString() + "/" + content.getName());

            if (session.exists(queueItemRef)) {
                log.error("Already created queue item : "
                        + content.getDestination() + " " + content.getName());
                return;
            }

            DocumentModel doc = session.createDocumentModel(
                    queueFolder.getPathAsString(), content.getName(),
                    QUEUE_ITEM_TYPE);
            doc.setProperty(QUEUEITEM_SCHEMA, QUEUEITEM_OWNER,
                    content.getOwner().toASCIIString());
            doc.setProperty(QUEUEITEM_SCHEMA, QUEUEITEM_SERVERID,
                    heartbeat.getMyURI().toASCIIString());
            doc = session.createDocument(doc);
            session.save();

        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.nuxeo.ecm.platform.queue.api.QueuePersister#setExecuteTime(org.nuxeo
     * .ecm.platform.queue.api.QueueContent, java.util.Date)
     */
    public void setExecuteTime(QueueContent content, Date date) {
        SetExecuteTimeRunner runner = new SetExecuteTimeRunner(
                Framework.getLocalService(RepositoryManager.class).getDefaultRepository().getName(),
                content, date);
        try {
            runner.runUnrestricted();
        } catch (ClientException e) {
            log.error("Couldn't set execution time to the content "
                    + content.getDestination() + ":" + content.getName(), e);
        }
    }

    class SetExecuteTimeRunner extends UnrestrictedSessionRunner {
        QueueContent content;

        Date executeTime;

        public SetExecuteTimeRunner(String repository, QueueContent content,
                Date executeTime) {
            super(repository);
            this.content = content;
            this.executeTime = executeTime;
        }

        @Override
        public void run() throws ClientException {
            DocumentModel queueFolder = getOrCreateQueue(session,
                    content.getDestination());
            DocumentRef queueItemRef = new PathRef(
                    queueFolder.getPathAsString() + "/" + content.getName());

            DocumentModel model = session.getDocument(queueItemRef);
            model.setProperty(QUEUEITEM_SCHEMA, QUEUEITEM_EXECUTE_TIME,
                    executeTime);
            model = session.saveDocument(model);
            session.save();
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.nuxeo.ecm.platform.queue.api.AtomicPersister#updateItem(org.nuxeo
     * .ecm.platform.queue.api.AtomicItem)
     */
    public void updateItem(QueueItem item) {
    }

    public DocumentModel getOrCreateQueue(CoreSession session, String queueName)
            throws ClientException {
        DocumentModel queueroot = getOrCreateRootQueueFolder(session);
        DocumentRef queueref = new PathRef(queueroot.getPathAsString() + "/"
                + queueName);

        if (!session.exists(queueref)) {
            DocumentModel model = session.createDocumentModel(
                    queueroot.getPathAsString(), queueName, QUEUE_TYPE);
            model = session.createDocument(model);
            session.save();
        }

        return session.getDocument(queueref);

    }

    public DocumentModel getOrCreateRootQueueFolder(CoreSession session)
            throws ClientException {
        DocumentRef queueRootDocRef = new PathRef("/" + QUEUE_ROOT_NAME);
        if (!session.exists(queueRootDocRef)) {
            DocumentModel model = session.createDocumentModel("/",
                    QUEUE_ROOT_NAME, QUEUE_ROOT_TYPE);
            model = session.createDocument(model);
            session.save();
        }

        return session.getDocument(queueRootDocRef);
    }

}
