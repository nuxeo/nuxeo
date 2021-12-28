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
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.core.api.LifeCycleConstants.DELETED_STATE;
import static org.nuxeo.ecm.core.api.LifeCycleConstants.DELETE_TRANSITION;
import static org.nuxeo.ecm.core.api.LifeCycleConstants.UNDELETE_TRANSITION;
import static org.nuxeo.ecm.core.api.trash.TrashService.Feature.TRASHED_STATE_IS_DEDUCED_FROM_LIFECYCLE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentExistsException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.trash.TrashInfo;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public abstract class AbstractTestTrashService {

    @Inject
    protected CoreSession session;

    @Inject
    protected TrashService trashService;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Inject
    protected CoreFeature coreFeature;

    protected DocumentModel fold;

    protected DocumentModel doc1;

    protected DocumentModel doc2;

    protected DocumentModel doc3;

    protected NuxeoPrincipal principal;

    @Before
    public void setUp() throws Exception {
        principal = session.getPrincipal();
    }

    public void createDocuments() {
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

    @Test
    public void testNameMangling() {
        createDocuments();
        String mangled = trashService.mangleName(doc1);
        assertTrue(mangled, mangled.startsWith("doc1._"));
        assertTrue(mangled, mangled.endsWith("_.trashed"));

        trashService.trashDocument(doc1);
        doc1 = session.getDocument(doc1.getRef());

        mangled = doc1.getName();
        assertTrue(mangled, mangled.startsWith("doc1._"));
        assertTrue(mangled, mangled.endsWith("_.trashed"));

        assertEquals("doc1", trashService.unmangleName(doc1));
    }

    @Test
    public void testBase() {
        createDocuments();
        assertTrue(trashService.folderAllowsDelete(fold));
        assertTrue(trashService.checkDeletePermOnParents(Arrays.asList(doc1, doc2)));
        assertTrue(trashService.canDelete(Collections.singletonList(fold), principal, false));
        assertTrue(trashService.canDelete(Collections.singletonList(doc1), principal, false));
        assertTrue(trashService.canDelete(Collections.singletonList(doc2), principal, false));
        assertFalse(trashService.canPurgeOrUntrash(Collections.singletonList(fold), principal));
        assertFalse(trashService.canPurgeOrUntrash(Collections.singletonList(doc1), principal));
        assertFalse(trashService.canPurgeOrUntrash(Collections.singletonList(doc2), principal));

        TrashInfo info = trashService.getTrashInfo(Arrays.asList(fold, doc1, doc3), principal, false, false);
        assertEquals(3, info.docs.size());
        assertEquals(2, info.rootRefs.size());
        assertEquals(new HashSet<>(Arrays.asList(new Path("/fold"), new Path("/doc3"))), info.rootPaths);

        DocumentModel above = trashService.getAboveDocument(doc1, Collections.singleton(new Path("/fold/doc1")));
        assertEquals(above.getPathAsString(), fold.getId(), above.getId());

        // test new method return the same result
        above = trashService.getAboveDocument(doc1, Collections.singleton(new Path("/fold/doc1")));
        assertEquals(above.getPathAsString(), fold.getId(), above.getId());
    }

    @Test
    public void testTrashPurgeUndelete() {
        createDocuments();
        // file with name from collision
        DocumentModel doc4 = session.createDocumentModel("/", "doc4.1400676936345", "Note");
        doc4 = session.createDocument(doc4);
        String doc4origname = doc4.getName();

        trashService.trashDocuments(Arrays.asList(fold, doc1, doc3, doc4));

        transactionalFeature.nextTransaction();

        // refetch as lifecycle state is cached
        fold = session.getDocument(new IdRef(fold.getId()));
        doc1 = session.getDocument(new IdRef(doc1.getId()));
        doc2 = session.getDocument(new IdRef(doc2.getId()));
        doc3 = session.getDocument(new IdRef(doc3.getId()));
        doc4 = session.getDocument(new IdRef(doc4.getId()));
        assertTrue(fold.isTrashed());
        assertTrue(doc1.isTrashed());
        assertTrue(doc3.isTrashed());
        assertTrue(doc4.isTrashed());
        // doc2 done by async BulkLifeCycleChangeListener
        assertTrue(doc2.isTrashed());
        // check names changed
        assertFalse("fold".equals(fold.getName()));
        assertFalse("doc1".equals(doc1.getName()));
        String doc3delname = doc3.getName();
        assertFalse("doc3".equals(doc3.getName()));
        assertFalse(doc4origname.equals(doc4.getName()));
        assertFalse("doc4".equals(doc4.getName()));
        // when recursing, don't change name
        assertEquals("doc2", doc2.getName());

        assertTrue(trashService.canPurgeOrUntrash(Arrays.asList(fold, doc1, doc2, doc3, doc4), principal));

        // purge doc1
        trashService.purgeDocuments(session, Collections.singletonList(doc1.getRef()));
        assertFalse(session.exists(doc1.getRef()));

        // untrash doc2 and doc4
        trashService.untrashDocuments(Arrays.asList(doc2, doc4));
        fold = session.getDocument(new IdRef(fold.getId()));
        doc2 = session.getDocument(new IdRef(doc2.getId()));
        doc4 = session.getDocument(new IdRef(doc4.getId()));
        assertFalse(doc2.isTrashed());
        assertFalse(doc4.isTrashed());
        // fold also untrashed
        assertFalse(fold.isTrashed());
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
        // untrash doc3
        trashService.untrashDocument(doc3);
        doc3 = session.getDocument(new IdRef(doc3.getId()));
        assertFalse(doc3.isTrashed());
        // check it was renamed again during untrash
        assertFalse("doc3".equals(doc3.getName()));
        assertFalse(doc3delname.equals(doc3.getName()));
    }

    @Test
    public void testTrashPurgeDocumentsUnder() {
        createDocuments();
        trashService.trashDocument(doc1);

        transactionalFeature.nextTransaction();

        assertFalse(session.isTrashed(fold.getRef()));
        assertTrue(session.isTrashed(doc1.getRef()));
        assertFalse(session.isTrashed(doc2.getRef()));
        assertFalse(session.isTrashed(doc3.getRef()));

        // doc1 is now trashed but not doc2 - purgeDocumentsUnder of fold should only purge doc1
        trashService.purgeDocumentsUnder(fold);

        transactionalFeature.nextTransaction();

        assertFalse(session.isTrashed(fold.getRef()));
        assertFalse(session.exists(doc1.getRef()));
        assertFalse(session.isTrashed(doc2.getRef()));
        assertFalse(session.isTrashed(doc3.getRef()));

    }

    // NXP-30417
    @Test
    public void testTrashPurgeDocumentsUnderWithPermission() {
        DocumentModel fold1 = session.createDocumentModel("/", "fold1", "Folder");
        fold1 = session.createDocument(fold1);
        DocumentModel fold2 = session.createDocumentModel("/fold1", "fold2", "Folder");
        fold2 = session.createDocument(fold2);
        ACP acp = fold2.getACP();
        acp.addACE(ACL.LOCAL_ACL, new ACE("bob", "ReadWrite", true));
        fold2.setACP(acp, true);
        DocumentModel doc1 = session.createDocumentModel("/fold1/fold2", "doc1", "File");
        doc1 = session.createDocument(doc1);
        transactionalFeature.nextTransaction();

        try (CloseableCoreSession bobSession = coreFeature.openCoreSession("bob")) {
            assertFalse(bobSession.exists(fold1.getRef()));
            assertTrue(bobSession.exists(fold2.getRef()));
            assertTrue(bobSession.exists(doc1.getRef()));

            trashService.trashDocument(doc1);
            transactionalFeature.nextTransaction();

            assertTrue(trashService.isTrashed(bobSession, doc1.getRef()));

            // fetch and purge fold2 with bob
            fold2 = bobSession.getDocument(fold2.getRef());
            trashService.purgeDocumentsUnder(fold2);
            transactionalFeature.nextTransaction();

            assertFalse(session.exists(doc1.getRef()));
        }
    }

    @Test
    public void testCannotTrashDocumentUnderLegalHold() {
        createDocuments();
        session.makeRecord(doc1.getRef());
        session.setLegalHold(doc1.getRef(), true, null);
        transactionalFeature.nextTransaction();
        try {
            trashService.trashDocument(doc1);
            fail("Document under legal hold cannot be trashed.");
        } catch (DocumentSecurityException e) {
            // expected
            assertEquals(
                    "User " + session.getPrincipal().getName() + " does not have the permission to remove the document "
                            + doc1.getId() + " (" + doc1.getPath() + ")",
                    e.getMessage());
        }
        try {
            trashService.trashDocument(fold);
            fail("Document with a descendant under legal hold cannot be trashed.");
        } catch (DocumentExistsException e) {
            // expected
            assertEquals("Cannot remove " + doc1.getId() + ", it is under retention / hold", e.getMessage());
        }
    }

    @Test
    public void testCannotTrashDocumentUnderRetention() {
        createDocuments();
        session.makeRecord(doc1.getRef());
        Calendar retainUntil = Calendar.getInstance();
        retainUntil.add(Calendar.DAY_OF_MONTH, 5);
        session.setRetainUntil(doc1.getRef(), retainUntil, "any comment");
        transactionalFeature.nextTransaction();
        try {
            trashService.trashDocument(doc1);
            fail("Document under retention cannot be trashed.");
        } catch (DocumentSecurityException e) {
            // expected
        }
        try {
            trashService.trashDocument(fold);
            fail("Document with a descendant under retention cannot be trashed.");
        } catch (DocumentExistsException e) {
            // expected
        }
    }

    @Test
    public void testUntrashChildren() {
        createDocuments();
        // trashing /fold/doc1 to mangle its name specifically
        trashService.trashDocument(doc1);
        // trashing /fold and recursively /fold/doc2
        trashService.trashDocument(fold);

        transactionalFeature.nextTransaction();

        // refetch as lifecycle state is cached
        fold = session.getDocument(new IdRef(fold.getId()));
        doc1 = session.getDocument(new IdRef(doc1.getId()));
        doc2 = session.getDocument(new IdRef(doc2.getId()));
        assertTrue(fold.isTrashed());
        // doc1 trashed explicitly
        assertTrue(doc1.isTrashed());
        // doc2 trashed recursively
        assertTrue(doc2.isTrashed());
        // fold is mangled as it was directly trashed
        assertTrue(trashService.isMangledName(fold.getName()));
        // doc1 is mangled as it was directly trashed
        assertTrue(trashService.isMangledName(doc1.getName()));
        // doc2 is not mangled as it was indirectly trashed
        assertFalse(trashService.isMangledName(doc2.getName()));

        // untrash fold
        trashService.untrashDocument(fold);

        transactionalFeature.nextTransaction();

        fold = session.getDocument(new IdRef(fold.getId()));
        doc1 = session.getDocument(new IdRef(doc1.getId()));
        doc2 = session.getDocument(new IdRef(doc2.getId()));
        assertFalse(fold.isTrashed());
        // children recursively untrashed
        assertFalse(doc1.isTrashed());
        assertFalse(doc2.isTrashed());
        // fold is unmangled
        assertFalse(trashService.isMangledName(fold.getName()));
        // doc1 is recursively unmangled
        assertFalse(trashService.isMangledName(doc1.getName()));
        // doc2 stays unmangled
        assertFalse(trashService.isMangledName(doc2.getName()));
    }

    @Test
    public void testTrashFolderContainingProxy() {
        createDocuments();
        DocumentRef versionRef = session.checkIn(doc3.getRef(), VersioningOption.MAJOR, null);
        DocumentModel version = session.getDocument(versionRef);
        DocumentModel proxy = session.createProxy(versionRef, fold.getRef());
        session.save();

        assertFalse(fold.isTrashed());
        assertFalse(proxy.isTrashed());
        assertFalse(version.isTrashed());

        // now trash the folder
        trashService.trashDocument(fold);

        transactionalFeature.nextTransaction();

        fold.refresh();
        version.refresh();
        assertTrue(fold.isTrashed());
        assertFalse(version.isTrashed());
        assertFalse(session.exists(proxy.getRef()));
    }

    @Test
    public void testProxy() {
        createDocuments();
        DocumentRef verRef = doc3.checkIn(null, null);
        DocumentModel proxy = session.createProxy(verRef, fold.getRef());
        session.save();
        assertTrue(trashService.canDelete(Collections.singletonList(proxy), principal, false));
        assertFalse(trashService.canDelete(Collections.singletonList(proxy), principal, true));
        assertFalse(trashService.canPurgeOrUntrash(Collections.singletonList(proxy), principal));
    }

    /**
     * @since 7.3
     */
    @Test
    public void testDeleteTwice() {
        createDocuments();
        List<DocumentModel> dd = new ArrayList<>();
        dd.add(doc1);
        trashService.trashDocuments(dd);
        trashService.trashDocuments(dd);
        assertTrue(session.exists(doc1.getRef()));
    }

    @Test
    public void testPlacelessDocument() {
        DocumentModel doc4 = session.createDocumentModel(null, "doc4", "Note");
        doc4 = session.createDocument(doc4);
        session.save();
        DocumentModel above = trashService.getAboveDocument(doc4, Collections.singleton(new Path("/")));
        assertNull(above);
        above = trashService.getAboveDocument(doc4, principal);
        assertNull(above);
        trashService.trashDocument(doc4);
        assertFalse(session.exists(doc4.getRef()));
    }

    @Test
    public void testTrashCheckedInDocumentDefault() {
        doTestTrashCheckedInDocument(true);
    }

    protected void doTestTrashCheckedInDocument(boolean expectCheckedIn) {
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/folder", "doc", "File");
        doc = session.createDocument(doc);

        // check in
        doc.checkIn(VersioningOption.MAJOR, null);
        assertFalse(doc.isCheckedOut());
        session.save();

        // trash
        trashService.trashDocument(doc);

        // make sure it's still checked in (or not if compat)
        doc = session.getDocument(new IdRef(doc.getId()));
        assertTrue(doc.isTrashed());
        if (expectCheckedIn) {
            assertFalse(doc.isCheckedOut());
        } else {
            assertTrue(doc.isCheckedOut());
        }

        // untrash
        trashService.untrashDocument(doc);

        // make sure it's still checked in (or not if compat)
        doc = session.getDocument(new IdRef(doc.getId()));
        assertFalse(doc.isTrashed());
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

    @Test
    public void testFollowTransitionBackwardCompatibility() {
        DocumentModel file = session.createDocumentModel("/", "file", "File");
        file = session.createDocument(file);
        session.save();
        DocumentRef fileRef = file.getRef();

        // following detete/undelete will trigger the trash service
        file.followTransition(DELETE_TRANSITION);
        // we don't follow transition anymore in new system
        if (trashService.hasFeature(TRASHED_STATE_IS_DEDUCED_FROM_LIFECYCLE)) {
            assertEquals(DELETED_STATE, session.getCurrentLifeCycleState(fileRef));
        } else {
            assertEquals("project", session.getCurrentLifeCycleState(fileRef));
        }
        assertTrue(session.isTrashed(fileRef));

        file.followTransition(UNDELETE_TRANSITION);
        // we don't follow transition anymore in new system
        // in all cases we're expecting project state
        assertEquals("project", session.getCurrentLifeCycleState(fileRef));
        assertFalse(session.isTrashed(fileRef));

    }

    /*
     * NXP-28935
     */
    @Test
    public void testUntrashThreeLevels() {
        DocumentModel folder1 = session.createDocumentModel("/", "folder1", "Folder");
        folder1 = session.createDocument(folder1);
        DocumentModel folder2 = session.createDocumentModel("/folder1", "folder2", "Folder");
        folder2 = session.createDocument(folder2);
        DocumentModel folder3 = session.createDocumentModel("/folder1/folder2", "folder3", "Folder");
        folder3 = session.createDocument(folder3);
        DocumentModel doc = session.createDocumentModel("/folder1/folder2/folder3", "doc", "File");
        doc = session.createDocument(doc);

        // trashing /folder1
        trashService.trashDocument(folder1);

        // children recursion is async
        transactionalFeature.nextTransaction();

        // refetch as lifecycle state is cached
        folder1.refresh();
        folder2.refresh();
        folder3.refresh();
        doc.refresh();

        assertTrue(folder1.isTrashed());
        assertTrue(folder2.isTrashed());
        assertTrue(folder3.isTrashed());
        assertTrue(doc.isTrashed());

        // untrashing /folder1
        trashService.untrashDocument(folder1);

        // children recursion is async
        transactionalFeature.nextTransaction();

        // refetch as lifecycle state is cached
        folder1.refresh();
        folder2.refresh();
        folder3.refresh();
        doc.refresh();

        assertFalse(folder1.isTrashed());
        assertFalse(folder2.isTrashed());
        assertFalse(folder3.isTrashed());
        assertFalse(doc.isTrashed());
    }
}
