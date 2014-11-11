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

import static org.nuxeo.ecm.platform.queue.core.DocumentQueueConstants.QUEUEITEM_BLACKLIST_TIME;
import static org.nuxeo.ecm.platform.queue.core.DocumentQueueConstants.QUEUEITEM_CONTENT;
import static org.nuxeo.ecm.platform.queue.core.DocumentQueueConstants.QUEUEITEM_EXECUTE_TIME;
import static org.nuxeo.ecm.platform.queue.core.DocumentQueueConstants.QUEUEITEM_EXECUTION_COUNT_PROPERTY;
import static org.nuxeo.ecm.platform.queue.core.DocumentQueueConstants.QUEUEITEM_OWNER;
import static org.nuxeo.ecm.platform.queue.core.DocumentQueueConstants.QUEUEITEM_SCHEMA;
import static org.nuxeo.ecm.platform.queue.core.DocumentQueueConstants.QUEUEITEM_SERVERID;
import static org.nuxeo.ecm.platform.queue.core.DocumentQueueConstants.QUEUE_ITEM_TYPE;
import static org.nuxeo.ecm.platform.queue.core.DocumentQueueConstants.QUEUE_ROOT_NAME;
import static org.nuxeo.ecm.platform.queue.core.DocumentQueueConstants.QUEUE_ROOT_TYPE;
import static org.nuxeo.ecm.platform.queue.core.DocumentQueueConstants.QUEUE_TYPE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.InputStreamBlob;
import org.nuxeo.ecm.core.management.storage.DocumentStoreManager;
import org.nuxeo.ecm.core.management.storage.DocumentStoreSessionRunner;
import org.nuxeo.ecm.platform.heartbeat.api.HeartbeatManager;
import org.nuxeo.ecm.platform.queue.api.QueueError;
import org.nuxeo.ecm.platform.queue.api.QueueInfo;
import org.nuxeo.ecm.platform.queue.api.QueuePersister;
import org.nuxeo.runtime.api.Framework;

/**
 * Persist a queue inside a nuxeo repository as a folder and files
 *
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
public class DocumentQueuePersister<C extends Serializable> implements QueuePersister<C> {

    public static final Log log = LogFactory.getLog(DocumentQueuePersister.class);

    protected PathRef rootPath() {
        return DocumentStoreManager.newPath(QUEUE_ROOT_NAME);
    }

    protected PathRef queuePath() {
        return DocumentStoreManager.newPath(QUEUE_ROOT_NAME, queueName);
    }

    protected DocumentModel queue(CoreSession session) throws ClientException {
        return session.getDocument(queuePath());
    }

    protected final String queueName;

    protected final Class<C> contentType;

    protected DocumentQueuePersister(String queueName, Class<C> contentType) {
        this.queueName = queueName;
        this.contentType = contentType;
    }

    @Override
    public void createIfNotExist() {
        new CreateIfNotExistRunner().runSafe();
    }


    protected class CreateIfNotExistRunner extends DocumentStoreSessionRunner {

        @Override
        public void run() throws ClientException {
            PathRef queuePath = queuePath();
            if (session.exists(queuePath)) {
                return;
            }
            PathRef rootPath = rootPath();
            DocumentModel root;
            if (!session.exists(rootPath)) {
                root = session.createDocumentModel(DocumentStoreManager.newPath().toString(), QUEUE_ROOT_NAME, QUEUE_ROOT_TYPE);
                root = session.createDocument(root);
            } else {
                root = session.getDocument(rootPath);
            }
            DocumentModel queue = session.createDocumentModel(rootPath.toString(), queueName, QUEUE_TYPE);
            queue = session.createDocument(queue);
            session.save();
        }
    }

    @Override
    public QueueInfo<C> removeContent(URI contentName) {
            RemoveRunner runner = new RemoveRunner(contentName);
            runner.runSafe();
            return new DocumentQueueAdapter<C>(runner.doc);
    }

    protected class RemoveRunner extends DocumentStoreSessionRunner {

        protected URI contentName;

        protected DocumentModel doc;

        RemoveRunner(URI contentName) {
            this.contentName = contentName;
        }

        @Override
        public void run() throws ClientException {
            PathRef ref= newPathRef(session, contentName);
            doc = session.getDocument(ref);
            detachDocument(doc);
            session.removeDocument(ref);
            session.save();
        }
    }


    @Override
    public boolean hasContent(URI name) {
            HasContentRunner runner = new HasContentRunner(name);
            runner.runSafe();
            return runner.hasContent;
    }

    protected class HasContentRunner extends DocumentStoreSessionRunner {

        protected URI contentName;

        protected boolean hasContent = false;

        protected HasContentRunner(URI contentName) {
            this.contentName =contentName;
        }

        @Override
        public void run() throws ClientException {
            PathRef ref = newPathRef(session,  contentName);
            hasContent = session.exists(ref);
        }
    }


    @SuppressWarnings("unchecked")
    protected  List<QueueInfo<C>> adapt(List<DocumentModel> docs) {
        List<QueueInfo<C>> queueItemList = new ArrayList<QueueInfo<C>>(docs.size());
        for (DocumentModel doc : docs) {
            queueItemList.add(doc.getAdapter(QueueInfo.class));
        }
        return queueItemList;
    }

    @Override
    public List<QueueInfo<C>> listKnownItems() {
        ListKnownContentRunner runner = new ListKnownContentRunner();
        runner.runSafe();
        return adapt(runner.docs);
    }

    protected class ListKnownContentRunner extends DocumentStoreSessionRunner {

        protected DocumentModelList docs;

        protected ListKnownContentRunner() {
            ;
        }

        @Override
        public void run() throws ClientException {
            DocumentModel queueDoc = queue(session);
            docs = session.getChildren(queueDoc.getRef());
            for (DocumentModel doc:docs) {
                detachDocument(doc);
            }
        }
    }

    @Override
    public QueueInfo<C> addContent(URI ownerName, URI name,  C content)  {
        SaveContentRunner runner = new SaveContentRunner(ownerName, name, content);
        runner.runSafe();
        return new DocumentQueueAdapter<C>(runner.doc);
    }

    protected  class SaveContentRunner extends DocumentStoreSessionRunner {

        protected DocumentModel doc;

        protected final URI contentName;

        protected final URI ownerName;

        protected final C content;

        protected SaveContentRunner(URI ownerName, URI name, C content) {
            this.contentName = name;
            this.ownerName = ownerName;
            this.content = content;
        }

        @Override
        public void run() throws ClientException {

            HeartbeatManager heartbeat = Framework.getLocalService(HeartbeatManager.class);
            PathRef ref = newPathRef(session, contentName);
            if (session.exists(ref)) {
                throw new QueueError("Already created queue item", contentName);
            }

             DocumentModel parent = queue(session);
             doc = session.createDocumentModel(parent.getPathAsString(), contentName.toASCIIString(), QUEUE_ITEM_TYPE);

            doc.setProperty(QUEUEITEM_SCHEMA, QUEUEITEM_OWNER,
                    ownerName.toASCIIString());
            doc.setProperty(QUEUEITEM_SCHEMA, QUEUEITEM_SERVERID,
                    heartbeat.getInfo().getId().toASCIIString());

            injectContent(doc, content);

            doc = session.createDocument(doc);
            detachDocument(doc);
            session.save();
        }
    }

    @Override
    public QueueInfo<C> setLaunched(URI contentName) {
        SetLaunchedRunner runner = new SetLaunchedRunner(contentName);
        runner.runSafe();
        return new DocumentQueueAdapter<C>(runner.doc);
    }



    protected class SetLaunchedRunner extends DocumentStoreSessionRunner {

        protected final URI contentName;

        protected  SetLaunchedRunner(URI contentName) {
            this.contentName = contentName;
        }

        protected DocumentModel doc;

        @Override
        public void run() throws ClientException {
            PathRef ref = newPathRef(session, contentName);
            doc = session.getDocument(ref);
            Long executionCount = (Long) doc.getPropertyValue(QUEUEITEM_EXECUTION_COUNT_PROPERTY);
            if (executionCount == null) {
                executionCount = 1L;
            } else {
                executionCount++;
            }
            doc.setProperty(QUEUEITEM_SCHEMA, QUEUEITEM_EXECUTE_TIME, new Date());
            doc.setPropertyValue(QUEUEITEM_EXECUTION_COUNT_PROPERTY, executionCount);
            doc = session.saveDocument(doc);
            detachDocument(doc);
            session.save();
        }

    }

    @Override
    public QueueInfo<C> setBlacklisted(URI contentName) {
        SetBlacklistedRunner runner = new SetBlacklistedRunner(contentName);
        runner.runSafe();
        return new DocumentQueueAdapter<C>(runner.doc);
    }

        protected class SetBlacklistedRunner extends DocumentStoreSessionRunner {

        protected final URI contentName;

        protected DocumentModel doc;

        protected  SetBlacklistedRunner(URI contentName) {
            this.contentName = contentName;
        }

        @Override
        public void run() throws ClientException {
            PathRef ref = newPathRef(session, contentName);
            doc = session.getDocument(ref);
            doc.setProperty(QUEUEITEM_SCHEMA, QUEUEITEM_BLACKLIST_TIME, new Date());
            doc = session.saveDocument(doc);
            detachDocument(doc);
            session.save();
        }

    }

    protected  class UpdateAdditionalInfosRunner extends DocumentStoreSessionRunner {

        protected final URI contentName;

        protected final C content;

        protected UpdateAdditionalInfosRunner(URI contentName, C content) {
            this.contentName = contentName;
            this.content = content;
        }

        @Override
        public void run() throws ClientException {
            PathRef ref = newPathRef(session, contentName);
            DocumentModel doc = session.getDocument(ref);
            injectContent(doc, content);
            doc = session.saveDocument(doc);
            session.save();
        }

    }


    @Override
    public void updateContent(URI contentName, C content) {
        new UpdateAdditionalInfosRunner(contentName, content).runSafe();
    }

    protected class ListByOwnerRunner extends DocumentStoreSessionRunner {

        protected URI ownerName;

        protected List<DocumentModel> docs;

       protected  ListByOwnerRunner(URI ownerName) {
            this.ownerName = ownerName;
        }

        @Override
        public void run() throws ClientException {
            DocumentModel queue = queue(session);
            String query = String.format("SELECT * FROM QueueItem WHERE ecm:parentId = '%s' AND  qitm:owner = '%s'",
                    queue.getId(),
                    ownerName.toASCIIString());
            docs = session.query(query);
            for (DocumentModel doc:docs) {
                detachDocument(doc);
            }
        }

    }

    @Override
    public List<QueueInfo<C>> listByOwner(URI ownerName) {
        ListByOwnerRunner runner = new ListByOwnerRunner(ownerName);
        runner.runSafe();
        return adapt(runner.docs);
    }

    protected  class RemoveByOwnerRunner extends DocumentStoreSessionRunner {

        protected int count;

        protected final URI ownerName;

       protected  RemoveByOwnerRunner(URI ownerName) {
            this.ownerName = ownerName;
        }

        @Override
        public void run() throws ClientException {
            DocumentModel queue = queue(session);
            String query = String.format("SELECT * FROM QueueItem WHERE ecm:parentId = '%s' AND  qitm:owner = '%s'",
                    queue.getId(),
                    ownerName.toASCIIString());
            List<DocumentModel> docs = session.query(query);
            for (DocumentModel doc:docs) {
                session.removeDocument(doc.getRef());
            }
            count = docs.size();
            session.save();
        }

    }


    @Override
    public int removeByOwner(URI ownerName) {
        RemoveByOwnerRunner runner = new RemoveByOwnerRunner(ownerName);
        runner.runSafe();
        return runner.count;
    }



    protected class GetInfoRunner extends DocumentStoreSessionRunner {

        protected URI contentName;

        protected DocumentModel doc;

       protected  GetInfoRunner(URI contentName) {
            this.contentName = contentName;
        }

        @Override
        public void run() throws ClientException {
            DocumentModel queue = queue(session);
            DocumentModel doc = session.getChild(queue.getRef(), contentName.toASCIIString());
            if (doc == null) {
                throw new QueueError("no such content", contentName);
            }
            detachDocument(doc);
        }

    }

    @Override
    public QueueInfo<C> getInfo(URI contentName) {
        GetInfoRunner runner = new GetInfoRunner(contentName);;
        runner.runSafe();
        return new DocumentQueueAdapter<C>(runner.doc);
    }

    protected static String formatTimestamp(Date date) {
        return new SimpleDateFormat("'TIMESTAMP' ''yyyy-MM-dd HH:mm:ss.SSS''").format(date);
    }

    protected class RemoveBlacklistedRunner extends DocumentStoreSessionRunner {

         final Date from;

         int removedCount;

         protected RemoveBlacklistedRunner(Date from) {
             this.from = from;
         }

        @Override
        public void run() throws ClientException {
            String ts = formatTimestamp(from);
            log.debug("Removing blacklisted doc oldest than " + ts);
            String req = String.format("SELECT * from QueueItem where qitm:blacklistTime < %s and ecm:isProxy = 0", ts);
            DocumentModelList docs = session.query(req);
            removedCount = docs.size();
            for (DocumentModel doc : docs) {
                log.debug("Removing blacklisted doc " + doc.getPathAsString());
                session.removeDocument(doc.getRef());
            }
            session.save();
        }

    }

    @Override
    public int removeBlacklisted(Date from) {
        RemoveBlacklistedRunner runner =new RemoveBlacklistedRunner(from);
        runner.runSafe();
        return runner.removedCount;
    }

    protected void injectContent(DocumentModel doc, Serializable content) throws ClientException {
        Blob blob = null;
        if (content != null) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(baos);
                out.writeObject(content);
                out.flush();
                blob = new InputStreamBlob(new ByteArrayInputStream(baos.toByteArray()));
            } catch (IOException e) {
                log.error("Couldn't write object", e);
            }
        }
        doc.setProperty(QUEUEITEM_SCHEMA, QUEUEITEM_CONTENT, blob);
    }

    protected  PathRef newPathRef(CoreSession session, URI name) throws ClientException {
        DocumentModel queueFolder = queue(session);
        return new PathRef(queueFolder.getPathAsString() + "/" + name.toASCIIString());
    }



    protected static void detachDocument(DocumentModel doc) throws ClientException {
        ((DocumentModelImpl) doc).detach(true);
    }


}
