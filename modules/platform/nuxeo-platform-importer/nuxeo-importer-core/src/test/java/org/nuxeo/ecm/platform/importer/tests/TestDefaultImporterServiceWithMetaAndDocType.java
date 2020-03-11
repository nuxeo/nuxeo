/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thibaud Arguillere
 */
package org.nuxeo.ecm.platform.importer.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.importer.service.DefaultImporterService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @author Thibaud Arguillere
 * @since 5.9.2
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.content.template")
@Deploy("org.nuxeo.ecm.platform.importer.core")
@Deploy("org.nuxeo.ecm.platform.importer.core.test:test-importer-service-contrib-metadata-with-doctype.xml")
public class TestDefaultImporterServiceWithMetaAndDocType {

    private static final String kIMPORT_FOLDER_NAME = "metadatas-with-doctype";

    private static final String kIMPORT_FOLDER_PATH = "/default-domain/workspaces/" + kIMPORT_FOLDER_NAME;

    private static final String kBRANCH_FOLDER_PATH = "/default-domain/workspaces/" + kIMPORT_FOLDER_NAME + "/branch1";

    @Inject
    protected CoreSession session;

    @Inject
    protected DefaultImporterService importerService;

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void testImporterContribution() throws Exception {
        File source = FileUtils.getResourceFileFromContext(kIMPORT_FOLDER_NAME);
        String targetPath = "/default-domain/workspaces/";

        importerService.importDocuments(targetPath, source.getPath(), false, 5, 5);

        session.save();
        txFeature.nextTransaction();

        DocumentModel docContainer = session.getDocument(new PathRef(kIMPORT_FOLDER_PATH));
        assertNotNull(docContainer);
        assertEquals("Folder", docContainer.getType());

        DocumentModel folder = session.getDocument(new PathRef(kIMPORT_FOLDER_PATH + "/branch1"));
        assertNotNull(folder);
        assertEquals("Folder", folder.getType());

        DocumentModel file = session.getDocument(new PathRef(kIMPORT_FOLDER_PATH + "/do_default.pdf"));
        assertNotNull(file);
        assertEquals("File", file.getType());
        assertEquals("src1", file.getPropertyValue("dc:source"));

        DocumentModel note = session.getDocument(new PathRef(kBRANCH_FOLDER_PATH + "/do_Note.html"));
        assertNotNull(note);
        assertEquals("Note", note.getType());
        assertTrue(note.hasFacet("testfacet"));
        assertEquals("the note", note.getPropertyValue("note:note"));

        /*
         * MailMessage not a default type... DocumentModel mailMess = session.getDocument(new PathRef(
         * kBRANCH_FOLDER_PATH + "/do_MailMessage.pdf")); assertNotNull(mailMess); assertEquals("MailMessage",
         * file.getType()); assertEquals("hop@la.boum", file.getPropertyValue("mail:sender"));
         */
    }

}
