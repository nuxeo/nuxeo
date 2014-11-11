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
import org.nuxeo.ecm.core.api.facet.VersioningDocument;

/**
 * Tests VersioningDocument adapter. The adapter is retrieved from a
 * DocumentModel object using VersioningAdapterFactory
 */
public class TestVersioningInitialVersionNumber extends VersioningBaseTestCase {

    public void testDefaultVersionNumber() throws DocumentException,
            ClientException {
        DocumentModel rootDM = session.getRootDocument();
        DocumentModel childFile = session.createDocumentModel(
                rootDM.getPathAsString(), "testfile1", "VerFile");

        DocumentModel doc = session.createDocument(childFile);
        VersioningDocument vdoc = doc.getAdapter(VersioningDocument.class);
        assertNotNull(vdoc);
        checkVersion(doc, 1L, 0L);
    }

    public void testVersionNotWellFormedNumber() throws Exception {

        deployContrib("org.nuxeo.ecm.platform.versioning.tests",
                "OSGI-INF/versioning-initial-version-number-contrib-bad.xml");

        DocumentModel rootDM = session.getRootDocument();
        DocumentModel childFile = session.createDocumentModel(
                rootDM.getPathAsString(), "testfile1", "VerFile");

        DocumentModel doc = session.createDocument(childFile);
        VersioningDocument vdoc = doc.getAdapter(VersioningDocument.class);
        assertNotNull(vdoc);
        checkVersion(doc, 0L, 0L);
    }

    public void testVersionNumber() throws Exception {

        deployContrib("org.nuxeo.ecm.platform.versioning.tests",
                "OSGI-INF/versioning-initial-version-number-contrib.xml");

        DocumentModel rootDM = session.getRootDocument();
        DocumentModel childFile = session.createDocumentModel(
                rootDM.getPathAsString(), "testfile1", "VerFile");

        DocumentModel doc = session.createDocument(childFile);
        VersioningDocument vdoc = doc.getAdapter(VersioningDocument.class);
        assertNotNull(vdoc);
        checkVersion(doc, 2L, 1L);
    }
}
