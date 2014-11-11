/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.versioning;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;

/**
 * Tests VersioningDocument adapter. The adapter is retrieved from a DocumentModel
 * object using VersioningAdapterFactory
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class TestVersioningAdapter extends VersioningBaseTestCase {

    private static final Log log = LogFactory.getLog(TestVersioningAdapter.class);

    private CoreSession coreSession;

    public void testNothing() {
    }

    @Override
    public void setUp() throws Exception {
        log.info("Initializing NX Core for local tests");
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.versioning.tests",
                "DocumentAdapterService.xml");
        openCoreSession();
    }

    @Override
    public void tearDown() throws Exception {
        log.info("Shutting down NX Core for local tests");
        super.tearDown();
    }

    @Override
    protected void openCoreSession() throws ClientException {
        Map<String, Serializable> context = new HashMap<String, Serializable>();
        context.put("username", "Administrator");
        coreSession = CoreInstance.getInstance().open("demo", context);
        assertNotNull(coreSession);
    }

    public void testVersionDocEditLockedState() throws DocumentException,
            ClientException {
        DocumentModel rootDM = coreSession.getRootDocument();

        DocumentModel childFile = coreSession.createDocumentModel(
                rootDM.getPathAsString(), "testfile1", "VerFile");

        // should fill datamodel
        childFile = coreSession.createDocument(childFile);

        DocumentModel doc = childFile;

        final VersioningDocument vdoc = doc.getAdapter(VersioningDocument.class);

        assertNotNull("Fail to get VersioningDocument adapter for document: "
                + doc.getTitle(), vdoc);

        checkVersion(doc, 1L, 0L);

        vdoc.incrementMinor();

        checkVersion(doc, 1L, 1L);

        coreSession.saveDocument(doc);
        coreSession.save();

        checkVersion(doc, 1L, 1L);

        vdoc.incrementMajor();
        coreSession.save();

        checkVersion(doc, 2L, 0L);
    }

    public void testDefinedRules() throws DocumentException, ClientException {
        DocumentModel rootDM = coreSession.getRootDocument();

        DocumentModel childFile = coreSession.createDocumentModel(
                rootDM.getPathAsString(), "testfile1", "VerFile");

        // should fill datamodel
        childFile = coreSession.createDocument(childFile);

        DocumentModel doc = childFile;

        final VersioningDocument vdoc = doc.getAdapter(VersioningDocument.class);

        assertNotNull("Fail to get VersioningDocument adapter for document: "
                + doc.getTitle(), vdoc);

        checkVersion(doc, 1L, 0L);

        DocumentRef docRef = doc.getRef();

        assertEquals("project", coreSession.getCurrentLifeCycleState(docRef));

        vdoc.incrementVersions();

        // XXX: checkVersion(doc, 1L, 1L);
    }

}
