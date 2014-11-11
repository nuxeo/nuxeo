/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Dragos Mihalache
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.versioning;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;

/**
 * @author Dragos Mihalache
 * @author Florent Guillaume
 */
public class TestVersioningDocEdit extends VersioningBaseTestCase {

    public void testVersionIncUsingService() throws ClientException {
        DocumentModel doc = session.createDocumentModel("/", "testfile1",
                "VerFile");
        doc = session.createDocument(doc);
        checkVersion(doc, 1, 0);

        versioningService.incrementMinor(doc);
        checkVersion(doc, 1, 1);

        session.saveDocument(doc);
        session.save();
        checkVersion(doc, 1, 1);

        versioningService.incrementMajor(doc);
        session.save();
        checkVersion(doc, 2, 0);
    }

    /**
     * Will test if the version is incremented in case the DocumentModel env
     * context is added with inc option.
     */
    public void testDocumentSaveWithIncOption() throws ClientException {
        DocumentModel doc = session.createDocumentModel("/", "testfile1",
                "VerFile");
        doc = session.createDocument(doc);
        checkVersion(doc, 1, 0);

        doc.putContextData(VersioningActions.KEY_FOR_INC_OPTION,
                VersioningActions.ACTION_INCREMENT_MINOR);
        doc = session.saveDocument(doc);
        checkVersion(doc, 1, 1);

        doc.putContextData(VersioningActions.KEY_FOR_INC_OPTION,
                VersioningActions.ACTION_INCREMENT_MAJOR);
        doc = session.saveDocument(doc);
        checkVersion(doc, 2, 0);
    }

    /**
     * Will test if the version is incremented in case the DocumentModel env
     * context is added with inc option.
     */
    public void testVersioningBySnapshotting() throws ClientException {
        DocumentModel doc = session.createDocumentModel("/", "testfile1",
                "VerFile");
        doc = session.createDocument(doc);
        checkVersion(doc, 1, 0);

        doc.putContextData(VersioningActions.KEY_FOR_INC_OPTION,
                VersioningActions.ACTION_INCREMENT_MINOR);
        doc = session.saveDocument(doc);
        checkVersion(doc, 1, 1);

        doc.putContextData(VersioningActions.KEY_FOR_INC_OPTION,
                VersioningActions.ACTION_INCREMENT_MAJOR);
        doc.putContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, Boolean.TRUE);
        doc = session.saveDocument(doc);
        checkVersion(doc, 2, 0);
    }

    /**
     * Test that edit after snapshotting will create a new version number.
     */
    public void testVersioningMultipleSnapshotting() throws ClientException {
        DocumentModel doc = session.createDocumentModel("/", "testfile1",
                "VerFile");
        doc = session.createDocument(doc);
        doc.setProperty("dublincore", "title", "A");
        session.saveDocument(doc);
        checkVersion(doc, 1, 0);

        // snapshot A=1.0 and save B
        doc.setProperty("dublincore", "title", "B");
        doc.putContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, Boolean.TRUE);
        doc = session.saveDocument(doc);
        checkVersion(doc, 1, 1);

        // another snapshot for B=1.1, using major inc
        doc.putContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, Boolean.TRUE);
        doc.putContextData(VersioningActions.KEY_FOR_INC_OPTION,
                VersioningActions.ACTION_INCREMENT_MAJOR);
        doc = session.saveDocument(doc);
        checkVersion(doc, 2, 0);

        // another snapshot doesn't change anything, doc is clean
        doc.putContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, Boolean.TRUE);
        // must clear option, otherwise it is kept (listener cannot clear it
        // because a copy is passed)
        doc.putContextData(VersioningActions.KEY_FOR_INC_OPTION, null);
        doc = session.saveDocument(doc);
        checkVersion(doc, 2, 0);

        // now snapshot+inc
        doc.putContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, Boolean.TRUE);
        doc.putContextData(VersioningActions.KEY_FOR_INC_OPTION,
                VersioningActions.ACTION_INCREMENT_MINOR);
        doc = session.saveDocument(doc);
        checkVersion(doc, 2, 1);

        // another save+inc, no snapshot
        doc.setProperty("dublincore", "title", "C");
        doc.putContextData(VersioningActions.KEY_FOR_INC_OPTION,
                VersioningActions.ACTION_INCREMENT_MAJOR);
        doc = session.saveDocument(doc);
        session.getLastVersion(doc.getRef());
        checkVersion(doc, 3, 0);
    }

    private void checkVersions(DocumentModel doc, String... labels)
            throws ClientException {
        List<String> actual = new LinkedList<String>();
        for (DocumentModel ver : session.getVersions(doc.getRef())) {
            assertTrue(ver.isVersion());
            actual.add(ver.getVersionLabel());
        }
        assertEquals(Arrays.asList(labels), actual);
        List<DocumentRef> versionsRefs = session.getVersionsRefs(doc.getRef());
        assertEquals(labels.length, versionsRefs.size());
    }

    public void testPublishVersioning() throws ClientException {
        DocumentModel folder = session.createDocumentModel("/", "folder",
                "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/", "file", "VerFile");
        doc = session.createDocument(doc);
        checkVersion(doc, 1, 0);
        checkVersions(doc);

        // publish
        DocumentModel proxy = session.publishDocument(doc, folder);
        checkVersion(proxy, 1, 0);
        checkVersion(doc, 1, 1);
        checkVersions(doc, "1");

        // republish, no new version
        proxy = session.publishDocument(doc, folder);
        checkVersion(proxy, 1, 0);
        checkVersion(doc, 1, 1);
        checkVersions(doc, "1");

        // do a change, and republish
        doc.setProperty("dublincore", "title", "A");
        session.saveDocument(doc);
        proxy = session.publishDocument(doc, folder);
        checkVersion(proxy, 1, 1);
        checkVersion(doc, 1, 2);
        checkVersions(doc, "1", "2");
    }

}
