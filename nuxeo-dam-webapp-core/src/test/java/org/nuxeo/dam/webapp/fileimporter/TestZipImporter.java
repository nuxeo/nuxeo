/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Quentin Lamerand
 *
 * $Id$
 */

package org.nuxeo.dam.webapp.fileimporter;

import java.io.File;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.filemanager.utils.FileManagerUtils;
import org.nuxeo.runtime.api.Framework;

public class TestZipImporter extends RepositoryOSGITestCase {

    protected FileManager service;

    protected DocumentModel root;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.platform.types.api");
        deployBundle("org.nuxeo.ecm.platform.types.core");
        deployBundle("org.nuxeo.ecm.platform.mimetype.core");
        deployBundle("org.nuxeo.ecm.platform.picture.api");
        deployBundle("org.nuxeo.ecm.platform.picture.core");

        deployContrib("org.nuxeo.ecm.platform.filemanager.core",
                "OSGI-INF/nxfilemanager-service.xml");
        deployContrib("org.nuxeo.ecm.platform.filemanager.core",
                "OSGI-INF/nxfilemanager-plugins-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.picture.web",
                "OSGI-INF/picturebook-types-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.picture.web",
                "OSGI-INF/imaging-various-contrib.xml");
        deployContrib("org.nuxeo.ecm.webapp.core",
                "OSGI-INF/nxfilemanager-plugins-contrib.xml");

        openRepository();
        service = Framework.getService(FileManager.class);
        root = coreSession.getRootDocument();
    }

    @Override
    public void tearDown() throws Exception {
        service = null;
        root = null;

        undeployContrib("org.nuxeo.ecm.webapp.core",
                "OSGI-INF/nxfilemanager-plugins-contrib.xml");
        undeployContrib("org.nuxeo.ecm.platform.picture.web",
                "OSGI-INF/imaging-various-contrib.xml");
        undeployContrib("org.nuxeo.ecm.platform.picture.web",
                "OSGI-INF/picturebook-types-contrib.xml");
        undeployContrib("org.nuxeo.ecm.platform.filemanager.core",
                "OSGI-INF/nxfilemanager-plugins-contrib.xml");
        undeployContrib("org.nuxeo.ecm.platform.filemanager.core",
                "OSGI-INF/nxfilemanager-service.xml");

        super.tearDown();
    }

    protected File getTestFile(String relativePath) {
        return new File(FileUtils.getResourcePathFromContext(relativePath));
    }

    public void testImportZip() throws Exception {
        File file = getTestFile("test-data/test.zip");

        byte[] content = FileManagerUtils.getBytesFromFile(file);
        ByteArrayBlob input = new ByteArrayBlob(content, "application/zip");

        DocumentModel doc = service.createDocumentFromBlob(coreSession, input,
                root.getPathAsString(), true, "test-data/test.zip");

        DocumentModel child = coreSession.getChild(doc.getRef(), "plain");
        assertNotNull(child);
        assertEquals("Note", child.getType());
        child = coreSession.getChild(doc.getRef(), "image");
        assertNotNull(child);
        assertEquals("Picture", child.getType());
        child = coreSession.getChild(doc.getRef(), "spreadsheet");
        assertNotNull(child);
        assertEquals("File", child.getType());
    }

}
