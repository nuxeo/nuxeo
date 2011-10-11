/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.trash.test;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.core.trash.TrashInfo;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.runtime.api.Framework;

public class TestTrashService extends SQLRepositoryTestCase {

    protected TrashService trashService;

    protected DocumentModel fold;

    protected DocumentModel doc1;

    protected DocumentModel doc2;

    protected DocumentModel doc3;

    protected Principal principal;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        openSession();
        trashService = Framework.getService(TrashService.class);
        principal = session.getPrincipal();
    }

    @Override
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    public void createDocuments() throws Exception {
        fold = session.createDocumentModel("/", "fold", "Folder");
        fold = session.createDocument(fold);
        doc1 = session.createDocumentModel("/fold", "doc1", "Note");
        doc1 = session.createDocument(doc1);
        doc2 = session.createDocumentModel("/fold", "doc2", "Note");
        doc2 = session.createDocument(doc2);
        doc3 = session.createDocumentModel("/", "doc3", "Note");
        doc3 = session.createDocument(doc3);
        session.save();
    }

    public void testBase() throws Exception {
        createDocuments();
        assertTrue(trashService.folderAllowsDelete(fold));
        assertTrue(trashService.checkDeletePermOnParents(Arrays.asList(doc1,
                doc2)));
        assertTrue(trashService.canDelete(Collections.singletonList(fold),
                principal, false));
        assertTrue(trashService.canDelete(Collections.singletonList(doc1),
                principal, false));
        assertTrue(trashService.canDelete(Collections.singletonList(doc2),
                principal, false));
        assertFalse(trashService.canPurgeOrUndelete(
                Collections.singletonList(fold), principal));
        assertFalse(trashService.canPurgeOrUndelete(
                Collections.singletonList(doc1), principal));
        assertFalse(trashService.canPurgeOrUndelete(
                Collections.singletonList(doc2), principal));

        TrashInfo info = trashService.getTrashInfo(Arrays.asList(fold, doc1,
                doc3), principal, false, false);
        assertEquals(3, info.docs.size());
        assertEquals(2, info.rootRefs.size());
        assertEquals(new HashSet<Path>(Arrays.asList(new Path("/fold"),
                new Path("/doc3"))), info.rootPaths);

        DocumentModel above = trashService.getAboveDocument(doc1,
                new HashSet<Path>(Arrays.asList(new Path("/fold/doc1"))));
        assertEquals(above.getPathAsString(), fold.getId(), above.getId());
    }

    public void testTrashPurgeUndelete() throws Exception {
        createDocuments();
        trashService.trashDocuments(Arrays.asList(fold, doc1));
        waitForEventsDispatched();
        session.save(); // fetch invalidations from async sessions

        // refetch as lifecycle state is cached
        fold = session.getDocument(new IdRef(fold.getId()));
        doc1 = session.getDocument(new IdRef(doc1.getId()));
        doc2 = session.getDocument(new IdRef(doc2.getId()));
        assertEquals("deleted", fold.getCurrentLifeCycleState());
        assertEquals("deleted", doc1.getCurrentLifeCycleState());
        // doc2 done by async BulkLifeCycleChangeListener
        assertEquals("deleted", doc2.getCurrentLifeCycleState());

        assertTrue(trashService.canPurgeOrUndelete(Arrays.asList(fold, doc1,
                doc2), principal));

        // purge doc1
        trashService.purgeDocuments(session,
                Collections.singletonList(doc1.getRef()));
        assertFalse(session.exists(doc1.getRef()));

        // undelete doc2
        trashService.undeleteDocuments(Collections.singletonList(doc2));
        fold = session.getDocument(new IdRef(fold.getId()));
        doc2 = session.getDocument(new IdRef(doc2.getId()));
        assertEquals("project", doc2.getCurrentLifeCycleState());
        // fold also undeleted
        assertEquals("project", fold.getCurrentLifeCycleState());
    }

    public void testUndeleteChildren() throws Exception {
        createDocuments();
        trashService.trashDocuments(Collections.singletonList(fold));
        waitForEventsDispatched();
        session.save(); // fetch invalidations from async sessions

        // refetch as lifecycle state is cached
        fold = session.getDocument(new IdRef(fold.getId()));
        doc1 = session.getDocument(new IdRef(doc1.getId()));
        doc2 = session.getDocument(new IdRef(doc2.getId()));
        assertEquals("deleted", fold.getCurrentLifeCycleState());
        // doc1 & doc2 done by async BulkLifeCycleChangeListener
        assertEquals("deleted", doc1.getCurrentLifeCycleState());
        assertEquals("deleted", doc2.getCurrentLifeCycleState());

        // undelete fold
        trashService.undeleteDocuments(Collections.singletonList(fold));
        waitForEventsDispatched();
        session.save(); // fetch invalidations from async sessions
        fold = session.getDocument(new IdRef(fold.getId()));
        doc1 = session.getDocument(new IdRef(doc1.getId()));
        doc2 = session.getDocument(new IdRef(doc2.getId()));
        assertEquals("project", fold.getCurrentLifeCycleState());
        // children done by async BulkLifeCycleChangeListener
        assertEquals("project", doc1.getCurrentLifeCycleState());
        assertEquals("project", doc2.getCurrentLifeCycleState());
    }

    private void waitForEventsDispatched() {
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
    }

}
