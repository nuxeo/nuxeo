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

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;

/**
 * Tests VersioningDocument adapter. The adapter is retrieved from a
 * DocumentModel object using VersioningAdapterFactory
 */
public class TestVersioningAdapter extends VersioningBaseTestCase {

    @SuppressWarnings("boxing")
    public void testVersionDocEditLockedState() throws DocumentException,
            ClientException {
        DocumentModel rootDM = session.getRootDocument();
        DocumentModel childFile = session.createDocumentModel(
                rootDM.getPathAsString(), "testfile1", "VerFile");
        // should fill datamodel
        DocumentModel doc = session.createDocument(childFile);
        VersioningDocument vdoc = doc.getAdapter(VersioningDocument.class);
        assertNotNull(vdoc);
        checkVersion(doc, 1L, 0L);

        vdoc.incrementMinor();
        checkVersion(doc, 1L, 1L);

        session.saveDocument(doc);
        session.save();
        checkVersion(doc, 1L, 1L);

        vdoc.incrementMajor();
        session.save();
        checkVersion(doc, 2L, 0L);
    }

    @SuppressWarnings("boxing")
    public void testDefinedRules() throws ClientException {
        DocumentModel rootDM = session.getRootDocument();
        DocumentModel childFile = session.createDocumentModel(
                rootDM.getPathAsString(), "testfile1", "VerFile");

        // should fill datamodel
        childFile = session.createDocument(childFile);
        DocumentModel doc = childFile;

        final VersioningDocument vdoc = doc.getAdapter(VersioningDocument.class);
        assertNotNull("Fail to get VersioningDocument adapter for document: "
                + doc.getTitle(), vdoc);
        checkVersion(doc, 1L, 0L);

        DocumentRef docRef = doc.getRef();
        assertEquals("project", session.getCurrentLifeCycleState(docRef));

        vdoc.incrementVersions();

        // XXX: checkVersion(doc, 1L, 1L);
    }

}
