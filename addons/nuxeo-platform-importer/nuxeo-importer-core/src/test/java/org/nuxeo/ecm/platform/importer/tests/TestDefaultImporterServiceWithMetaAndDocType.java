/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thibaud Arguillere
 */
package org.nuxeo.ecm.platform.importer.tests;

import static org.junit.Assert.*;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.importer.service.DefaultImporterService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Thibaud Arguillere
 *
 * @since 5.9.2
 */
public class TestDefaultImporterServiceWithMetaAndDocType extends SQLRepositoryTestCase {
    private static final Log log = LogFactory.getLog(TestDefaultImporterServiceWithMetaAndDocType.class);

    public static final String IMPORTER_CORE_TEST_BUNDLE = "org.nuxeo.ecm.platform.importer.core.test";

    public static final String IMPORTER_CORE_BUNDLE = "org.nuxeo.ecm.platform.importer.core";

    private static final String kIMPORT_FOLDER_NAME = "metadatas-with-doctype";
    private static final String kIMPORT_FOLDER_PATH = "/default-domain/workspaces/" + kIMPORT_FOLDER_NAME;
    private static final String kBRANCH_FOLDER_PATH = "/default-domain/workspaces/" + kIMPORT_FOLDER_NAME + "/branch1";

    public TestDefaultImporterServiceWithMetaAndDocType() {
        super();
    }

    protected TestDefaultImporterServiceWithMetaAndDocType(String name) {
        super(name);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        fireFrameworkStarted();
        openSession();
    }

    @Override
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    @Override
    protected void deployRepositoryContrib() throws Exception {
        super.deployRepositoryContrib();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.platform.content.template");
        deployBundle(IMPORTER_CORE_BUNDLE);
        deployContrib(IMPORTER_CORE_TEST_BUNDLE,
                "test-importer-service-contrib-metadata-with-doctype.xml");
    }

    @Test
    public void testImporterContribution() throws Exception {
        DefaultImporterService importerService = Framework.getService(DefaultImporterService.class);
        assertNotNull(importerService);

        File source = FileUtils.getResourceFileFromContext(kIMPORT_FOLDER_NAME);
        String targetPath = "/default-domain/workspaces/";

        importerService.importDocuments(targetPath, source.getPath(), false, 5,
                5);

        session.save();

        DocumentModel docContainer = session.getDocument(new PathRef(
                kIMPORT_FOLDER_PATH));
        assertNotNull(docContainer);
        assertEquals("Folder", docContainer.getType());

        DocumentModel folder = session.getDocument(new PathRef(
                kIMPORT_FOLDER_PATH + "/branch1"));
        assertNotNull(folder);
        assertEquals("Folder", folder.getType());

        DocumentModel file = session.getDocument(new PathRef(
                kIMPORT_FOLDER_PATH + "/do_default.pdf"));
        assertNotNull(file);
        assertEquals("File", file.getType());
        assertEquals("src1", file.getPropertyValue("dc:source"));

        DocumentModel note = session.getDocument(new PathRef(
                kBRANCH_FOLDER_PATH + "/do_Note.html"));
        assertNotNull(note);
        assertEquals("Note", note.getType());
        assertTrue(note.hasFacet("testfacet"));
        assertEquals("the note", note.getPropertyValue("note:note"));

        /* MailMessage not a default type...
        DocumentModel mailMess = session.getDocument(new PathRef(
                kBRANCH_FOLDER_PATH + "/do_MailMessage.pdf"));
        assertNotNull(mailMess);
        assertEquals("MailMessage", file.getType());
        assertEquals("hop@la.boum", file.getPropertyValue("mail:sender"));
        */
    }

}
