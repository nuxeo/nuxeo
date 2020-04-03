/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.wopi.lock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ_WRITE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE_PROPERTIES;
import static org.nuxeo.wopi.Constants.LOCK_DIRECTORY_FILE_ID;
import static org.nuxeo.wopi.Constants.LOCK_DIRECTORY_SCHEMA_NAME;
import static org.nuxeo.wopi.Constants.LOCK_DIRECTORY_TIMESTAMP;
import static org.nuxeo.wopi.Constants.LOCK_EXPIRATION_EVENT;
import static org.nuxeo.wopi.Constants.LOCK_TTL;
import static org.nuxeo.wopi.TestConstants.FILE_CONTENT_PROPERTY;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.wopi.FileInfo;
import org.nuxeo.wopi.WOPIFeature;

/**
 * Lock related tests.
 *
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features(WOPIFeature.class)
public class TestLock {

    @Inject
    protected CoreSession session;

    @Inject
    protected EventProducer eventProducer;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected UserManager userManager;

    @Inject
    protected TransactionalFeature transactionalFeature;

    protected DocumentModel doc;

    protected DocumentModel expiredLockDoc;

    protected String fileId;

    protected String expiredLockFileId;

    @Before
    public void setUp() {
        doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);
        fileId = FileInfo.computeFileId(doc, FILE_CONTENT_PROPERTY);

        expiredLockDoc = session.createDocumentModel("/", "expiredLockDoc", "File");
        expiredLockDoc = session.createDocument(expiredLockDoc);
        expiredLockFileId = FileInfo.computeFileId(expiredLockDoc, FILE_CONTENT_PROPERTY);
    }

    @Test
    public void testLockDirectory() {
        // get unknown lock
        assertNull(LockHelper.getLock("unknownFileId"));

        // is not locked
        assertFalse(LockHelper.isLocked(doc.getRepositoryName(), doc.getId()));
        assertFalse(LockHelper.isLocked(fileId));

        // add lock / get lock / is locked
        LockHelper.addLock(fileId, "wopiLock");
        assertEquals("wopiLock", LockHelper.getLock(fileId));
        assertTrue(LockHelper.isLocked(doc.getRepositoryName(), doc.getId()));
        assertTrue(LockHelper.isLocked(fileId));

        // update lock
        LockHelper.updateLock(fileId, "updatedWopiLock");
        assertEquals("updatedWopiLock", LockHelper.getLock(fileId));

        // refresh lock
        LockHelper.doPrivilegedOnLockDirectory(session -> {
            try {
                long timestamp = (long) session.getEntry(fileId).getProperty(LOCK_DIRECTORY_SCHEMA_NAME,
                        LOCK_DIRECTORY_TIMESTAMP);
                Thread.sleep(10); // NOSONAR
                LockHelper.refreshLock(fileId);
                long refreshedTimestamp = (long) session.getEntry(fileId).getProperty(LOCK_DIRECTORY_SCHEMA_NAME,
                        LOCK_DIRECTORY_TIMESTAMP);
                assertTrue(refreshedTimestamp > timestamp);
            } catch (InterruptedException e) {
                fail("Couldn't sleep to ensure the lock is refreshed");
            }
        });

        // remove lock
        LockHelper.removeLock(fileId);
        assertNull(LockHelper.getLock(fileId));

        // get expired locks
        LockHelper.addLock(fileId, "wopiLock");
        assertEquals("wopiLock", LockHelper.getLock(fileId));

        LockHelper.addLock(expiredLockFileId, "expiredWopiLock");
        assertEquals("expiredWopiLock", LockHelper.getLock(expiredLockFileId));

        Map<String, List<DocumentModel>> expiredLocksByRepository = LockHelper.doPrivilegedOnLockDirectory(
                LockHelper::getExpiredLocksByRepository);
        assertEquals(1, expiredLocksByRepository.size());
        String repositoryName = session.getRepositoryName();
        List<DocumentModel> expiredLocks = expiredLocksByRepository.get(repositoryName);
        assertNotNull(expiredLocks);
        assertTrue(expiredLocks.isEmpty());

        // force expiration for expiredLockFileId
        LockHelper.doPrivilegedOnLockDirectory(session -> {
            DocumentModel entry = session.getEntry(expiredLockFileId);
            long timestamp = (long) entry.getProperty(LOCK_DIRECTORY_SCHEMA_NAME, LOCK_DIRECTORY_TIMESTAMP);
            entry.setProperty(LOCK_DIRECTORY_SCHEMA_NAME, LOCK_DIRECTORY_TIMESTAMP,
                    timestamp - (LOCK_TTL + TimeUnit.MINUTES.toMillis(10)));
            session.updateEntry(entry);
        });
        expiredLocksByRepository = LockHelper.doPrivilegedOnLockDirectory(LockHelper::getExpiredLocksByRepository);
        expiredLocks = expiredLocksByRepository.get(repositoryName);
        assertEquals(1, expiredLocks.size());
        assertEquals(expiredLockFileId,
                expiredLocks.get(0).getProperty(LOCK_DIRECTORY_SCHEMA_NAME, LOCK_DIRECTORY_FILE_ID));
    }

    @Test
    public void testLockExpiration() {
        // lock documents
        doc.setLock();
        LockHelper.addLock(fileId, "foo");
        assertTrue(doc.isLocked());
        assertEquals("foo", LockHelper.getLock(fileId));

        expiredLockDoc.setLock();
        LockHelper.addLock(expiredLockFileId, "bar");
        assertTrue(expiredLockDoc.isLocked());
        assertEquals("bar", LockHelper.getLock(expiredLockFileId));

        // force expiration for expiredLockFileId
        LockHelper.doPrivilegedOnLockDirectory(session -> {
            DocumentModel entry = session.getEntry(expiredLockFileId);
            long timestamp = (long) entry.getProperty(LOCK_DIRECTORY_SCHEMA_NAME, LOCK_DIRECTORY_TIMESTAMP);
            entry.setProperty(LOCK_DIRECTORY_SCHEMA_NAME, LOCK_DIRECTORY_TIMESTAMP,
                    timestamp - (LOCK_TTL + TimeUnit.MINUTES.toMillis(10)));
            session.updateEntry(entry);
        });
        fireLockExpirationEvent();
        transactionalFeature.nextTransaction();
        doc = session.getDocument(doc.getRef());
        expiredLockDoc = session.getDocument(expiredLockDoc.getRef());
        assertTrue(doc.isLocked());
        assertNotNull(LockHelper.getLock(fileId));
        assertFalse(expiredLockDoc.isLocked());
        assertNull(LockHelper.getLock(expiredLockFileId));
    }

    @Test
    public void testCollaborativeEdition() {
        NuxeoPrincipal johnPrincipal = setUpPrincipal("john");
        CoreSession johnSession = coreFeature.getCoreSession(johnPrincipal);
        // no lock -> Write permission granted to john
        assertTrue(johnSession.hasPermission(doc.getRef(), WRITE_PROPERTIES));

        // lock document as joe
        NuxeoPrincipal joePrincipal = setUpPrincipal("joe");
        CoreSession joeSession = coreFeature.getCoreSession(joePrincipal);
        doc = joeSession.getDocument(doc.getRef());
        doc.setLock();
        assertTrue(joeSession.hasPermission(doc.getRef(), WRITE_PROPERTIES));

        // not a WOPI lock -> Write permission denied to john
        assertFalse(johnSession.hasPermission(doc.getRef(), WRITE_PROPERTIES));

        // add a WOPI lock as joe
        LockHelper.addLock(fileId, "foo");
        assertTrue(LockHelper.isLocked(doc.getRepositoryName(), doc.getId()));

        // WOPI lock but not a WOPI request -> Write permission denied to john
        assertFalse(johnSession.hasPermission(doc.getRef(), WRITE_PROPERTIES));

        // WOPI lock and a WOPI request -> Write permission granted to john
        // This is possible thanks to the WOPI lock security policy that grants access if the doc is locked by a
        // WOPI client (existing WOPI lock) and the request originates from a WOPI client.
        LockHelper.flagWOPIRequest();
        assertTrue(LockHelper.isWOPIRequest());
        assertTrue(johnSession.hasPermission(doc.getRef(), WRITE_PROPERTIES));

        // Unflag request
        LockHelper.unflagWOPIRequest();
        assertFalse(LockHelper.isWOPIRequest());
    }

    @Test
    public void testLockRemovalOnNuxeoUnlock() {
        // add WOPI lock and lock document in Nuxeo
        LockHelper.addLock(fileId, "foo");
        doc.setLock();
        assertTrue(doc.isLocked());
        assertEquals("foo", LockHelper.getLock(fileId));

        // unlock document in Nuxeo, expect WOPI lock removal
        doc.removeLock();
        assertFalse(doc.isLocked());
        assertNull(LockHelper.getLock(fileId));

        // lock document in Nuxeo
        doc.setLock();
        assertTrue(doc.isLocked());
        assertNull(LockHelper.getLock(fileId));

        // unlock document in Nuxeo, expect no side effect
        doc.removeLock();
        assertFalse(doc.isLocked());
        assertNull(LockHelper.getLock(fileId));
    }

    protected void fireLockExpirationEvent() {
        EventContext eventContext = new EventContextImpl();
        Event event = eventContext.newEvent(LOCK_EXPIRATION_EVENT);
        eventProducer.fireEvent(event);
    }

    protected NuxeoPrincipal setUpPrincipal(String username) {
        // grant Write access to the user on the root document
        DocumentModel rootDocument = session.getRootDocument();
        ACP acp = rootDocument.getACP();
        ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
        localACL.add(new ACE(username, READ_WRITE, true));
        rootDocument.setACP(acp, true);
        // build a NuxeoPrincipal
        NuxeoPrincipal principal = new NuxeoPrincipalImpl(username);
        DocumentModel model = userManager.getBareUserModel();
        model.setPropertyValue("user:username", username);
        principal.setModel(model);
        return principal;
    }

}
