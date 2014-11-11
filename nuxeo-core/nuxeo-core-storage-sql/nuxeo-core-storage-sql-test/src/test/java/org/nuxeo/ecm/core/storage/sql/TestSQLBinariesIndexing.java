package org.nuxeo.ecm.core.storage.sql;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.storage.sql.coremodel.BinaryTextListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class TestSQLBinariesIndexing extends TXSQLRepositoryTestCase {

    protected static final Log log = LogFactory.getLog(TestSQLBinariesIndexing.class);

    protected static CountDownLatch startIndexation;

    protected DocumentModel doc;

    Object originalPoolSize;
    Object originalMaxPoolSize;

    protected void setEventAsyncPoolSizes() {
        Properties properties = System.getProperties();
        originalPoolSize = properties.setProperty("org.nuxeo.ecm.core.event.async.poolSize", "1");
        originalMaxPoolSize = properties.setProperty("org.nuxeo.ecm.core.event.async.maxPoolSize", "1");
    }

    protected void restoreEventAsyncPoolSizes() {
        Properties properties = System.getProperties();
        if (originalPoolSize == null) {
            properties.remove("org.nuxeo.ecm.core.event.async.poolSize");
        } else {
            properties.put("org.nuxeo.ecm.core.event.async.poolSize", originalPoolSize);
        }
        if (originalMaxPoolSize == null) {
            properties.remove("org.nuxeo.ecm.core.event.async.maxPoolSize");
        } else {
            properties.put("org.nuxeo.ecm.core.event.async.maxPoolSize", originalMaxPoolSize);
        }
    }


    @Override
    public void setUp() throws Exception {
        setEventAsyncPoolSizes();
        startIndexation = new CountDownLatch(1);
        super.setUp();
        populate(session);
        docsAreNotIndexed();
    }

    @Override
    public void tearDown() throws Exception {
        startIndexation.countDown();
        restoreEventAsyncPoolSizes();
        super.tearDown();
    }

    @Override
    protected void deployRepositoryContrib() throws Exception {
        super.deployRepositoryContrib();
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.core.convert.plugins");
        deployBundle("org.nuxeo.ecm.core.storage.sql");
        deployContrib("org.nuxeo.ecm.core.storage.sql.test.tests", "OSGI-INF/test-asynch-binaries-indexing-contrib.xml");
    }

    public void populate(CoreSession repo) throws ClientException {
        doc = repo.createDocumentModel("/", "source", "File");
        BlobHolder holder = doc.getAdapter(BlobHolder.class);
        holder.setBlob(new StringBlob("test"));
        doc = repo.createDocument(doc);
        ((DocumentModelImpl) doc).detach(true);
    }

    public static class SynchHandler implements PostCommitEventListener {

        public void handleEvent(EventBundle events) throws ClientException {
            for (Event event : events) {
                if (BinaryTextListener.EVENT_NAME.equals(event.getName())) {
                    try {
                        TestSQLBinariesIndexing.startIndexation.await();
                    } catch (InterruptedException e) {
                        throw new ClientException("Cannot wait for test", e);
                    }
                }
            }
        }

    }

    protected void flushAndCommit() throws ClientException {
        session.save();
        closeSession();
        session = null;
        TransactionHelper.commitOrRollbackTransaction();
    }

    protected void recycleSession() throws Exception {
        if (session != null) {
            flushAndCommit();
        }
        TransactionHelper.startTransaction();
        openSession();
    }

    protected List<DocumentModel> indexedDocs() throws ClientException {
        return session.query("SELECT * FROM Document WHERE ecm:fulltext = 'test'");
    }

    protected List<DocumentModel> requestedDocs() throws ClientException {
        List<DocumentModel> requested = session.query("SELECT * from Document where ecm:fulltextJobId = '".concat(doc.getId()).concat("'"));
        return requested;
    }

    protected void waitForIndexing() throws Exception {

        flushAndCommit(); // flush and commit transaction

        startIndexation.countDown();

        Framework.getLocalService(EventService.class).waitForAsyncCompletion();

        startIndexation = new CountDownLatch(1);

        recycleSession();

    }

    protected void docsAreNotIndexed() throws Exception {

        recycleSession();

        assertEquals(0, indexedDocs().size());

    }

    public void testBinariesAreIndexed() throws Exception {
        assertEquals(1, requestedDocs().size());
        assertEquals(0, indexedDocs().size());

        waitForIndexing();

        assertEquals(0, requestedDocs().size());
        assertEquals(1, indexedDocs().size());
    }

    public void testCopiesAreIndexed() throws Exception {
            assertEquals(1, requestedDocs().size());
            assertEquals(0, indexedDocs().size());

            session.copy(doc.getRef(), session.getRootDocument().getRef(), "copy").getRef();

            recycleSession();

            // check copy is part of requested
            assertEquals(2, requestedDocs().size());

            waitForIndexing();

            // check other doc is indexed also
            assertEquals(0, requestedDocs().size());
            assertEquals(2, indexedDocs().size());

            // check other doc is not indexed twice
            DocumentModel rehydratedDoc = session.getDocument(doc.getRef());
            rehydratedDoc.getAdapter(BlobHolder.class).setBlob(new StringBlob("other"));
            session.saveDocument(rehydratedDoc);

            recycleSession();

            waitForIndexing();

            assertEquals(1, indexedDocs().size());
    }

    public void testVersionsAreIndexed() throws Exception {
        assertEquals(1, requestedDocs().size());
        assertEquals(0, indexedDocs().size());
        
        VersionModel version = new VersionModelImpl();
        version.setLabel("test");
        session.checkIn(doc.getRef(), version);

        waitForIndexing();

        assertEquals(2, indexedDocs().size());
   }
}
