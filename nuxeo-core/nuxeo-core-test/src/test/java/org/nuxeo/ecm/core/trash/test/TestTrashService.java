/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.trash.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.trash.TrashInfo;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestTrashService {

    @Inject
    protected CoreSession session;

    @Inject
    protected TrashService trashService;

    @Inject
    protected EventService eventService;

    protected DocumentModel fold;

    protected DocumentModel doc1;

    protected DocumentModel doc2;

    protected DocumentModel doc3;

    protected Principal principal;

    @Before
    public void setUp() throws Exception {
        principal = session.getPrincipal();
    }

    public void createDocuments() throws Exception {
        fold = session.createDocumentModel("/", "fold", "Folder");
        // use File as document type as Note is now automatically versioned on each update
        fold = session.createDocument(fold);
        doc1 = session.createDocumentModel("/fold", "doc1", "File");
        doc1 = session.createDocument(doc1);
        doc2 = session.createDocumentModel("/fold", "doc2", "File");
        doc2 = session.createDocument(doc2);
        doc3 = session.createDocumentModel("/", "doc3", "File");
        doc3 = session.createDocument(doc3);
        session.save();
    }

    protected void nextTransaction() {
        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
        TransactionHelper.startTransaction();
    }

    @Test
    public void testNameMangling() throws Exception {
        createDocuments();
        String mangled = trashService.mangleName(doc1);
        assertTrue(mangled, mangled.startsWith("doc1._"));
        assertTrue(mangled, mangled.endsWith("_.trashed"));

        trashService.trashDocuments(Collections.singletonList(doc1));
        doc1 = session.getDocument(doc1.getRef());

        mangled = doc1.getName();
        assertTrue(mangled, mangled.startsWith("doc1._"));
        assertTrue(mangled, mangled.endsWith("_.trashed"));

        assertEquals("doc1", trashService.unmangleName(doc1));
    }

    @Test
    public void testBase() throws Exception {
        createDocuments();
        assertTrue(trashService.folderAllowsDelete(fold));
        assertTrue(trashService.checkDeletePermOnParents(Arrays.asList(doc1, doc2)));
        assertTrue(trashService.canDelete(Collections.singletonList(fold), principal, false));
        assertTrue(trashService.canDelete(Collections.singletonList(doc1), principal, false));
        assertTrue(trashService.canDelete(Collections.singletonList(doc2), principal, false));
        assertFalse(trashService.canPurgeOrUndelete(Collections.singletonList(fold), principal));
        assertFalse(trashService.canPurgeOrUndelete(Collections.singletonList(doc1), principal));
        assertFalse(trashService.canPurgeOrUndelete(Collections.singletonList(doc2), principal));

        TrashInfo info = trashService.getTrashInfo(Arrays.asList(fold, doc1, doc3), principal, false, false);
        assertEquals(3, info.docs.size());
        assertEquals(2, info.rootRefs.size());
        assertEquals(new HashSet<Path>(Arrays.asList(new Path("/fold"), new Path("/doc3"))), info.rootPaths);

        DocumentModel above = trashService.getAboveDocument(doc1,
                new HashSet<Path>(Arrays.asList(new Path("/fold/doc1"))));
        assertEquals(above.getPathAsString(), fold.getId(), above.getId());
    }

    @Test
    public void testTrashPurgeUndelete() throws Exception {
        createDocuments();
        // file with name from collision
        DocumentModel doc4 = session.createDocumentModel("/", "doc4.1400676936345", "Note");
        doc4 = session.createDocument(doc4);
        String doc4origname = doc4.getName();

        trashService.trashDocuments(Arrays.asList(fold, doc1, doc3, doc4));

        nextTransaction();

        // refetch as lifecycle state is cached
        fold = session.getDocument(new IdRef(fold.getId()));
        doc1 = session.getDocument(new IdRef(doc1.getId()));
        doc2 = session.getDocument(new IdRef(doc2.getId()));
        doc3 = session.getDocument(new IdRef(doc3.getId()));
        doc4 = session.getDocument(new IdRef(doc4.getId()));
        assertEquals("deleted", fold.getCurrentLifeCycleState());
        assertEquals("deleted", doc1.getCurrentLifeCycleState());
        assertEquals("deleted", doc3.getCurrentLifeCycleState());
        assertEquals("deleted", doc4.getCurrentLifeCycleState());
        // doc2 done by async BulkLifeCycleChangeListener
        assertEquals("deleted", doc2.getCurrentLifeCycleState());
        // check names changed
        assertFalse("fold".equals(fold.getName()));
        assertFalse("doc1".equals(doc1.getName()));
        String doc3delname = doc3.getName();
        assertFalse("doc3".equals(doc3.getName()));
        assertFalse(doc4origname.equals(doc4.getName()));
        assertFalse("doc4".equals(doc4.getName()));
        // when recursing, don't change name
        assertEquals("doc2", doc2.getName());

        assertTrue(trashService.canPurgeOrUndelete(Arrays.asList(fold, doc1, doc2, doc3, doc4), principal));

        // purge doc1
        trashService.purgeDocuments(session, Collections.singletonList(doc1.getRef()));
        assertFalse(session.exists(doc1.getRef()));

        // undelete doc2 and doc4
        trashService.undeleteDocuments(Arrays.asList(doc2, doc4));
        fold = session.getDocument(new IdRef(fold.getId()));
        doc2 = session.getDocument(new IdRef(doc2.getId()));
        doc4 = session.getDocument(new IdRef(doc4.getId()));
        assertEquals("project", doc2.getCurrentLifeCycleState());
        assertEquals("project", doc4.getCurrentLifeCycleState());
        // fold also undeleted
        assertEquals("project", fold.getCurrentLifeCycleState());
        // check name restored
        assertEquals("fold", fold.getName());
        // name still unchanged
        assertEquals("doc2", doc2.getName());
        // doc4 was restored with a pristine name
        assertEquals("doc4", doc4.getName());

        // create a new file with same name as old doc3
        DocumentModel doc3bis = session.createDocumentModel("/", "doc3", "Note");
        doc3bis = session.createDocument(doc3bis);
        assertEquals("doc3", doc3bis.getName());
        // undelete doc3
        trashService.undeleteDocuments(Collections.singletonList(doc3));
        doc3 = session.getDocument(new IdRef(doc3.getId()));
        assertEquals("project", doc3.getCurrentLifeCycleState());
        // check it was renamed again during undelete
        assertFalse("doc3".equals(doc3.getName()));
        assertFalse(doc3delname.equals(doc3.getName()));
    }

    @Test
    public void testUndeleteChildren() throws Exception {
        createDocuments();
        trashService.trashDocuments(Collections.singletonList(fold));

        nextTransaction();

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

        nextTransaction();

        fold = session.getDocument(new IdRef(fold.getId()));
        doc1 = session.getDocument(new IdRef(doc1.getId()));
        doc2 = session.getDocument(new IdRef(doc2.getId()));
        assertEquals("project", fold.getCurrentLifeCycleState());
        // children done by async BulkLifeCycleChangeListener
        assertEquals("project", doc1.getCurrentLifeCycleState());
        assertEquals("project", doc2.getCurrentLifeCycleState());
    }

    @Test
    public void testTrashFolderContainingProxy() throws Exception {
        createDocuments();
        DocumentRef versionRef = session.checkIn(doc3.getRef(), VersioningOption.MAJOR, null);
        DocumentModel version = session.getDocument(versionRef);
        DocumentModel proxy = session.createProxy(versionRef, fold.getRef());
        session.save();

        assertEquals("project", fold.getCurrentLifeCycleState());
        assertEquals("project", proxy.getCurrentLifeCycleState());
        assertEquals("project", version.getCurrentLifeCycleState());

        // now delete the folder
        trashService.trashDocuments(Collections.singletonList(fold));

        nextTransaction();

        fold.refresh();
        version.refresh();
        assertEquals("deleted", fold.getCurrentLifeCycleState());
        assertEquals("project", version.getCurrentLifeCycleState());
        assertFalse(session.exists(proxy.getRef()));
    }

    @Test
    public void testProxy() throws Exception {
        createDocuments();
        DocumentRef verRef = doc3.checkIn(null, null);
        DocumentModel proxy = session.createProxy(verRef, fold.getRef());
        session.save();
        assertTrue(trashService.canDelete(Collections.singletonList(proxy), principal, false));
        assertFalse(trashService.canDelete(Collections.singletonList(proxy), principal, true));
        assertFalse(trashService.canPurgeOrUndelete(Collections.singletonList(proxy), principal));
    }

    /**
     * @since 7.3
     */
    @Test
    public void testDeleteTwice() throws Exception {
        createDocuments();
        List<DocumentModel> dd = new ArrayList<DocumentModel>();
        dd.add(doc1);
        trashService.trashDocuments(dd);
        trashService.trashDocuments(dd);
        assertTrue(session.exists(doc1.getRef()));
    }

    @Test
    public void testPlacelessDocument() throws Exception {
        DocumentModel doc4 = session.createDocumentModel(null, "doc4", "Note");
        doc4 = session.createDocument(doc4);
        session.save();
        DocumentModel above = trashService.getAboveDocument(doc4, new HashSet<Path>(Arrays.asList(new Path("/"))));
        assertNull(above);
        trashService.trashDocuments(Collections.singletonList(doc4));
        assertFalse(session.exists(doc4.getRef()));
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-trash-checkin-keep.xml")
    public void testTrashCheckedInDocumentKeepCheckedIn() throws Exception {
        doTestTrashCheckedInDocument(true);
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-trash-checkin-dontkeep.xml")
    public void testTrashCheckedInDocumentDontKeepCheckedIn() throws Exception {
        doTestTrashCheckedInDocument(false);
    }

    // default platform behavior
    @Test
    public void testTrashCheckedInDocumentDefault() throws Exception {
        doTestTrashCheckedInDocument(true);
    }

    protected void doTestTrashCheckedInDocument(boolean expectCheckedIn) throws Exception {
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/folder", "doc", "File");
        doc = session.createDocument(doc);

        // check in
        doc.checkIn(VersioningOption.MAJOR, null);
        assertFalse(doc.isCheckedOut());
        session.save();

        // trash
        trashService.trashDocuments(Collections.singletonList(doc));

        // make sure it's still checked in (or not if compat)
        doc = session.getDocument(new IdRef(doc.getId()));
        assertEquals(LifeCycleConstants.DELETED_STATE, doc.getCurrentLifeCycleState());
        if (expectCheckedIn) {
            assertFalse(doc.isCheckedOut());
        } else {
            assertTrue(doc.isCheckedOut());
        }

        // undelete
        trashService.undeleteDocuments(Collections.singletonList(doc));

        // make sure it's still checked in (or not if compat)
        doc = session.getDocument(new IdRef(doc.getId()));
        assertEquals("project", doc.getCurrentLifeCycleState());
        if (expectCheckedIn) {
            assertFalse(doc.isCheckedOut());
        } else {
            assertTrue(doc.isCheckedOut());
        }

        // another checked in doc
        DocumentModel doc2 = session.createDocumentModel("/folder", "doc2", "File");
        doc2 = session.createDocument(doc2);
        doc2.checkIn(VersioningOption.MAJOR, null);
        session.save();

        // following a non-delete transition does a checkout like any other modification
        session.followTransition(doc2, "approve");
        doc2 = session.getDocument(new IdRef(doc2.getId()));
        assertEquals("approved", doc2.getCurrentLifeCycleState());
        assertTrue(doc2.isCheckedOut());
    }

}
