/*
 * (C) Copyright 2006-2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     tmartins
 *
 */

package org.nuxeo.ecm.platform.filemanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.List;

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
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(repositoryName = "default", init = RepositoryInit.class, user = "Administrator", cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.types.api",
        "org.nuxeo.ecm.platform.types.core",
        "org.nuxeo.ecm.platform.filemanager.core",
        "org.nuxeo.ecm.platform.versioning.api",
        "org.nuxeo.ecm.platform.versioning" })
public class TestFileManagerVersioning {

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
    }

    @Test
    public void testIncorrectVersioningOption() throws Exception {
        harness.deployContrib("org.nuxeo.ecm.platform.filemanager.core.tests",
                "nxfilemanager-incorrect-versioning-contrib.xml");
        assertEquals(VersioningOption.MINOR, service.getVersioningOption());
    }

    @Test
    public void testCreateDocumentTwiceWithBlob() throws Exception {
        harness.deployContrib("org.nuxeo.ecm.platform.filemanager.core.tests",
                "nxfilemanager-versioning-contrib.xml");
        assertEquals(VersioningOption.MAJOR, service.getVersioningOption());

        // create doc
        File file = getTestFile("test-data/hello.doc");

        byte[] content = FileManagerUtils.getBytesFromFile(file);
        ByteArrayBlob input = new ByteArrayBlob(content, "application/msword");

        DocumentModel doc = service.createDocumentFromBlob(coreSession, input,
                root.getPathAsString(), true, "test-data/hello.doc");
        DocumentRef docRef = doc.getRef();

        assertNotNull(doc);
        assertEquals("hello.doc", doc.getProperty("dublincore", "title"));
        assertEquals("hello.doc", doc.getProperty("file", "filename"));
        assertNotNull(doc.getProperty("file", "content"));

        List<DocumentModel> versions = coreSession.getVersions(docRef);
        assertEquals(0, versions.size());

        // create again with same file
        doc = service.createDocumentFromBlob(coreSession, input,
                root.getPathAsString(), true, "test-data/hello.doc");
        assertNotNull(doc);

        DocumentRef newDocRef = doc.getRef();
        assertEquals(docRef, newDocRef);
        assertEquals("hello.doc", doc.getProperty("dublincore", "title"));
        assertEquals("hello.doc", doc.getProperty("file", "filename"));
        assertNotNull(doc.getProperty("file", "content"));

        versions = coreSession.getVersions(docRef);
        assertEquals(1, versions.size());

        VersioningManager vm = Framework.getLocalService(VersioningManager.class);
        String vl = vm.getVersionLabel(doc);
        assertEquals("1.0", vl);
    }

    protected File getTestFile(String relativePath) {
        return new File(FileUtils.getResourcePathFromContext(relativePath));
    }

}