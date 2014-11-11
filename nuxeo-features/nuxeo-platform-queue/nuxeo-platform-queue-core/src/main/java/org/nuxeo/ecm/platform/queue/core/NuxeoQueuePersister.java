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

import static org.nuxeo.ecm.platform.queue.core.NuxeoQueueConstants.QUEUEITEM_CONTENT;
import static org.nuxeo.ecm.platform.queue.core.NuxeoQueueConstants.QUEUEITEM_EXECUTE_TIME;
import static org.nuxeo.ecm.platform.queue.core.NuxeoQueueConstants.QUEUEITEM_EXECUTION_COUNT_PROPERTY;
import static org.nuxeo.ecm.platform.queue.core.NuxeoQueueConstants.QUEUEITEM_OWNER;
import static org.nuxeo.ecm.platform.queue.core.NuxeoQueueConstants.QUEUEITEM_SCHEMA;
import static org.nuxeo.ecm.platform.queue.core.NuxeoQueueConstants.QUEUEITEM_SERVERID;
import static org.nuxeo.ecm.platform.queue.core.NuxeoQueueConstants.QUEUE_ITEM_TYPE;
import static org.nuxeo.ecm.platform.queue.core.NuxeoQueueConstants.QUEUE_ROOT_NAME;
import static org.nuxeo.ecm.platform.queue.core.NuxeoQueueConstants.QUEUE_ROOT_TYPE;
import static org.nuxeo.ecm.platform.queue.core.NuxeoQueueConstants.QUEUE_TYPE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
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
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.InputStreamBlob;
import org.nuxeo.ecm.platform.heartbeat.api.ServerHeartBeat;
import org.nuxeo.ecm.platform.queue.api.QueueError;
import org.nuxeo.ecm.platform.queue.api.QueueInfo;
import org.nuxeo.ecm.platform.queue.api.QueuePersister;
import org.nuxeo.runtime.api.Framework;

/**
 * Persist a queue inside a nuxeo repository as a folder and files
 *
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
public class NuxeoQueuePersister<C extends Serializable> implements QueuePersister<C> {

    public static final Log log = LogFactory.getLog(NuxeoQueuePersister.class);

    protected abstract class NuxeoQueueRunner extends UnrestrictedSessionRunner {

        protected URI name;

        public NuxeoQueueRunner() {
            super(NuxeoRepositoryNameProvider.getRepositoryName());
        }

        @Override
        public void runUnrestricted()  {
            try {
                super.runUnrestricted();
            } catch (ClientException e) {
                throw new QueueError("Error while executing " + getClass().getCanonicalName(), e, name);
            }
        }

    }

    protected final String queueName;

    protected final Class<C> contentType;

    protected NuxeoQueuePersister(String queueName, Class<C> contentType) {
        this.queueName = queueName;
        this.contentType = contentType;
    }

    @Override
    public QueueInfo<C> removeContent(URI contentName) {
            RemoveRunner runner = new RemoveRunner(contentName);
            runner.runUnrestricted();
            return new NuxeoQueueAdapter<C>(runner.doc);
    }

    protected class RemoveRunner extends NuxeoQueueRunner {

        protected DocumentModel doc;

        RemoveRunner(URI contentName) {
            this.name = contentName;
        }

        @Override
        public void run() throws ClientException {
            PathRef ref= newPathRef(session, name);
            doc = session.getDocument(ref);
            detachDocument(doc);
            session.removeDocument(ref);
            session.save();
        }
    }


    @Override
    public boolean hasContent(URI name) {
            HasContentRunner runner = new HasContentRunner(name);
            runner.runUnrestricted();
            return runner.hasContent;
    }

    protected class HasContentRunner extends NuxeoQueueRunner {

        protected boolean hasContent = false;

        protected HasContentRunner(URI contentName) {
            this.name =contentName;
        }

        @Override
        public void run() throws ClientException {
            PathRef ref = newPathRef(session,  name);
            hasContent = session.exists(ref);
        }
    }

    @Override
    public List<QueueInfo<C>> listKnownItems() {
        ListKnownContentRunner runner = new ListKnownContentRunner();
        runner.runUnrestricted();
        return adapt(runner.docs);
    }

    @SuppressWarnings("unchecked")
    protected  List<QueueInfo<C>> adapt(List<DocumentModel> docs) {
        List<QueueInfo<C>> queueItemList = new ArrayList<QueueInfo<C>>(docs.size());
        for (DocumentModel doc : docs) {
            queueItemList.add(doc.getAdapter(QueueInfo.class));
        }
        return queueItemList;
    }

    protected class ListKnownContentRunner extends NuxeoQueueRunner {

        protected DocumentModelList docs;

        protected ListKnownContentRunner() {
            ;
        }

        @Override
        public void run() throws ClientException {
            DocumentModel queueDoc = getOrCreateQueue(session);
            docs = session.getChildren(queueDoc.getRef());
            for (DocumentModel doc:docs) {
                detachDocument(doc);
            }
        }
    }

    @Override
    public QueueInfo<C> addContent(URI ownerName, URI name,  C content)  {
        SaveContentRunner runner = new SaveContentRunner(ownerName, name, content);
        runner.runUnrestricted();
        return new NuxeoQueueAdapter<C>(runner.doc);
    }

    protected  class SaveContentRunner extends NuxeoQueueRunner {

        protected DocumentModel doc;

        protected final URI ownerName;

        protected final C content;

        protected SaveContentRunner(URI ownerName, URI name, C content) {
            this.name = name;
            this.ownerName = ownerName;
            this.content = content;
        }

        @Override
        public void run() throws ClientException {

            ServerHeartBeat heartbeat = Framework.getLocalService(ServerHeartBeat.class);
            PathRef ref = newPathRef(session, name);
            if (session.exists(ref)) {
                throw new QueueError("Already created queue item", name);
            }

             DocumentModel parent = getOrCreateQueue(session);
             doc = session.createDocumentModel(parent.getPathAsString(), name.toASCIIString(), QUEUE_ITEM_TYPE);

            doc.setProperty(QUEUEITEM_SCHEMA, QUEUEITEM_OWNER,
                    ownerName.toASCIIString());
            doc.setProperty(QUEUEITEM_SCHEMA, QUEUEITEM_SERVERID,
                    heartbeat.getMyURI().toASCIIString());

            injectContent(doc, content);

            doc = session.createDocument(doc);
            detachDocument(doc);
            session.save();
        }
    }

    @Override
    public void setExecuteTime(URI contentName, Date date) {
        SetExecutionInfoRunner runner = new SetExecutionInfoRunner(contentName, date);
        runner.runUnrestricted();
    }

    protected class SetExecutionInfoRunner extends NuxeoQueueRunner {

        protected final Date executeTime;

        protected  SetExecutionInfoRunner(URI contentName, Date executeTime) {
            this.name = contentName;
            this.executeTime = executeTime;
        }

        @Override
        public void run() throws ClientException {
            PathRef ref = newPathRef(session, name);
            DocumentModel model = session.getDocument(ref);
            Long executionCount = (Long) model.getPropertyValue(QUEUEITEM_EXECUTION_COUNT_PROPERTY);
            if (executionCount == null) {
                executionCount = 1L;
            } else {
                executionCount++;
            }
            model.setProperty(QUEUEITEM_SCHEMA, QUEUEITEM_EXECUTE_TIME,
                    executeTime);
            model.setPropertyValue(QUEUEITEM_EXECUTION_COUNT_PROPERTY,
                    executionCount);
            model = session.saveDocument(model);
            session.save();
        }

    }

    protected  class UpdateAdditionalInfosRunner extends NuxeoQueueRunner {

        protected final C content;

        protected UpdateAdditionalInfosRunner(URI contentName, C content) {
            this.name = contentName;
            this.content = content;
        }

        @Override
        public void run() throws ClientException {
            PathRef ref = newPathRef(session, name);
            DocumentModel doc = session.getDocument(ref);
            injectContent(doc, content);
            doc = session.saveDocument(doc);
            session.save();
        }

    }


    @Override
    public void updateContent(URI contentName, C content) {
        UpdateAdditionalInfosRunner runner = new UpdateAdditionalInfosRunner(contentName, content);
        runner.runUnrestricted();
    }

    protected class ListByOwnerRunner extends NuxeoQueueRunner {

        protected URI ownerName;

        protected List<DocumentModel> docs;

       protected  ListByOwnerRunner(URI ownerName) {
            this.ownerName = ownerName;
        }

        @Override
        public void run() throws ClientException {
            DocumentModel queue = getOrCreateQueue(session);
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
        runner.runUnrestricted();
        return adapt(runner.docs);
    }

    protected  class RemoveByOwnerRunner extends NuxeoQueueRunner {

        protected int count;

        protected final URI ownerName;

       protected  RemoveByOwnerRunner(URI ownerName) {
            this.ownerName = ownerName;
        }

        @Override
        public void run() throws ClientException {
            DocumentModel queue = getOrCreateQueue(session);
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
        runner.runUnrestricted();
        return runner.count;
    }



    protected class GetInfoRunner extends NuxeoQueueRunner {

        protected DocumentModel doc;

       protected  GetInfoRunner(URI contentName) {
            this.name = contentName;
        }

        @Override
        public void run() throws ClientException {
            DocumentModel queue = getOrCreateQueue(session);
            DocumentModel doc = session.getChild(queue.getRef(), name.toASCIIString());
            if (doc == null) {
                throw new QueueError("no such content", name);
            }
            detachDocument(doc);
        }

    }

    @Override
    public QueueInfo<C> getInfo(URI contentName) {
        GetInfoRunner runner = new GetInfoRunner(contentName);;
        runner.runUnrestricted();
        return new NuxeoQueueAdapter<C>(runner.doc);
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

    protected PathRef newPathRef(CoreSession session, URI name) throws ClientException {
        DocumentModel queueFolder = getOrCreateQueue(session);
        return new PathRef(queueFolder.getPathAsString() + "/" + name.toASCIIString());
    }

    protected DocumentModel getOrCreateQueue(CoreSession session)
            throws ClientException {
        DocumentModel queueroot = getOrCreateRootQueueFolder(session);
        DocumentRef queueref = new PathRef(queueroot.getPathAsString() + "/" + queueName);

        if (!session.exists(queueref)) {
            DocumentModel model = session.createDocumentModel(queueroot.getPathAsString(),
                    queueName, QUEUE_TYPE);
            model = session.createDocument(model);
            session.save();
        }

        return session.getDocument(queueref);

    }

    protected DocumentModel getOrCreateRootQueueFolder(CoreSession session)
            throws ClientException {
        DocumentRef queueRootDocRef = new PathRef("/" + QUEUE_ROOT_NAME);
        if (!session.exists(queueRootDocRef)) {
            DocumentModel model = session.createDocumentModel("/", QUEUE_ROOT_NAME, QUEUE_ROOT_TYPE);
            model = session.createDocument(model);
            session.save();
        }

        return session.getDocument(queueRootDocRef);
    }



    protected static void detachDocument(DocumentModel doc) throws ClientException {
        ((DocumentModelImpl) doc).detach(true);
    }


}
