/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thierry Martins
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.filemanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.filemanager.utils.FileManagerUtils;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = RepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.types.api",
        "org.nuxeo.ecm.platform.types.core",
        "org.nuxeo.ecm.platform.filemanager.core",
        "org.nuxeo.ecm.platform.dublincore",
        "org.nuxeo.ecm.platform.versioning.api",
        "org.nuxeo.ecm.platform.versioning" })
@LocalDeploy("org.nuxeo.ecm.platform.types.core:ecm-types-test-contrib.xml")
public class TestFileManagerVersioning {

    private static final String TEST_BUNDLE = "org.nuxeo.ecm.platform.filemanager.core.tests";

    private static final String INCORRECT_XML = "nxfilemanager-incorrect-versioning-contrib.xml";

    private static final String CONTRIB_XML = "nxfilemanager-versioning-contrib.xml";

    private static final String CONTRIB2_XML = "nxfilemanager-versioning2-contrib.xml";

    private static final String HELLO_DOC = "test-data/hello.doc";

    private static final String APPLICATION_MSWORD = "application/msword";

    protected FileManager service;

    @Inject
    protected CoreSession coreSession;

    protected DocumentModel root;

    @Inject
    protected RuntimeHarness harness;

    @Before
    public void setUp() throws Exception {
        service = Framework.getLocalService(FileManager.class);
        root = coreSession.getRootDocument();
        // createWorkspaces();
    }

    @Test
    public void testDefaultVersioningOption() {
        assertEquals(VersioningOption.MINOR, service.getVersioningOption());
        assertFalse(service.doVersioningAfterAdd());
    }

    @Test
    public void testIncorrectVersioningOption() throws Exception {
        harness.deployContrib(TEST_BUNDLE, INCORRECT_XML);
        assertEquals(VersioningOption.MINOR, service.getVersioningOption());
        assertFalse(service.doVersioningAfterAdd());
        harness.undeployContrib(TEST_BUNDLE, INCORRECT_XML);
    }

    @Test
    public void testCreateDocumentNoVersioningAfterAdd() throws Exception {
        harness.deployContrib(TEST_BUNDLE, CONTRIB_XML);
        assertEquals(VersioningOption.MAJOR, service.getVersioningOption());
        assertFalse(service.doVersioningAfterAdd());

        // create doc
        File file = getTestFile(HELLO_DOC);
        byte[] content = FileManagerUtils.getBytesFromFile(file);
        ByteArrayBlob input = new ByteArrayBlob(content, APPLICATION_MSWORD);
        DocumentModel doc = service.createDocumentFromBlob(coreSession, input,
                root.getPathAsString(), true, HELLO_DOC);
        DocumentRef docRef = doc.getRef();

        assertNotNull(doc.getPropertyValue("file:content"));
        assertTrue(doc.isCheckedOut());
        assertEquals(0, coreSession.getVersions(docRef).size());
        assertEquals("0.0", doc.getVersionLabel());

        // overwrite file
        doc = service.createDocumentFromBlob(coreSession, input,
                root.getPathAsString(), true, HELLO_DOC);

        assertTrue(doc.isCheckedOut());
        assertEquals(1, coreSession.getVersions(docRef).size());
        assertEquals("1.0+", doc.getVersionLabel());

        // overwrite again
        doc = service.createDocumentFromBlob(coreSession, input,
                root.getPathAsString(), true, HELLO_DOC);

        assertTrue(doc.isCheckedOut());
        assertEquals(2, coreSession.getVersions(docRef).size());
        assertEquals("2.0+", doc.getVersionLabel());
        harness.undeployContrib(TEST_BUNDLE, CONTRIB_XML);
    }

    @Test
    public void testCreateDocumentVersioningAfterAdd() throws Exception {
        harness.deployContrib(TEST_BUNDLE, CONTRIB2_XML);
        assertEquals(VersioningOption.MINOR, service.getVersioningOption());
        assertTrue(service.doVersioningAfterAdd());

        // create doc
        File file = getTestFile(HELLO_DOC);
        byte[] content = FileManagerUtils.getBytesFromFile(file);
        ByteArrayBlob input = new ByteArrayBlob(content, APPLICATION_MSWORD);
        DocumentModel doc = service.createDocumentFromBlob(coreSession, input,
                root.getPathAsString(), true, HELLO_DOC);
        DocumentRef docRef = doc.getRef();

        assertNotNull(doc.getPropertyValue("file:content"));
        assertFalse(doc.isCheckedOut());
        assertEquals(1, coreSession.getVersions(docRef).size());
        assertEquals("0.1", doc.getVersionLabel());

        // overwrite file
        doc = service.createDocumentFromBlob(coreSession, input,
                root.getPathAsString(), true, HELLO_DOC);

        assertFalse(doc.isCheckedOut());
        assertEquals(2, coreSession.getVersions(docRef).size());
        assertEquals("0.2", doc.getVersionLabel());

        // overwrite again
        doc = service.createDocumentFromBlob(coreSession, input,
                root.getPathAsString(), true, HELLO_DOC);

        assertFalse(doc.isCheckedOut());
        assertEquals(3, coreSession.getVersions(docRef).size());
        assertEquals("0.3", doc.getVersionLabel());
        harness.undeployContrib(TEST_BUNDLE, CONTRIB2_XML);
    }

    protected File getTestFile(String relativePath) {
        return new File(FileUtils.getResourcePathFromContext(relativePath));
    }

}