/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Mariana Cedica
 */
package org.nuxeo.ecm.platform.importer.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.content.template")
@Deploy("org.nuxeo.ecm.platform.importer.core")
@Deploy("org.nuxeo.ecm.platform.importer.core.test:test-importer-service-contrib.xml")
public class TestDefaultImporterServiceWithMeta {

    @Inject
    protected CoreSession session;

    @Inject
    protected DefaultImporterService importerService;

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void testImporterContribution() throws Exception {
        File source = FileUtils.getResourceFileFromContext("import-src-with-metadatas");
        String targetPath = "/default-domain/workspaces/";

        importerService.importDocuments(targetPath, source.getPath(), false, 5, 5);
        assertEquals(true, importerService.getEnablePerfLogging());

        session.save();
        txFeature.nextTransaction();

        DocumentModel docContainer = session.getDocument(new PathRef(
                "/default-domain/workspaces/import-src-with-metadata"));
        assertNotNull(docContainer);
        assertEquals("Folder", docContainer.getType());

        DocumentModel folder = session.getDocument(new PathRef(
                "/default-domain/workspaces/import-src-with-metadata/branch1"));
        assertNotNull(folder);
        assertEquals("Folder", folder.getType());

        DocumentModel file = session.getDocument(new PathRef(
                "/default-domain/workspaces/import-src-with-metadata/hello.pdf"));
        assertNotNull(file);
        assertEquals("File", file.getType());
        assertEquals("src1", file.getPropertyValue("dc:source"));

        DocumentModel file1 = session.getChild(folder.getRef(), "hello1.pdf");
        assertEquals("src1-hello1", file1.getPropertyValue("dc:source"));

        DocumentModel folder11 = session.getChild(folder.getRef(), "branch11");

        DocumentModel file11 = session.getChild(folder11.getRef(), "hello11.pdf");
        String[] contributors = (String[]) file11.getPropertyValue("dc:subjects");
        assertNotNull(contributors);
        assertEquals("subject6", contributors[0]);
        assertEquals("subject7", contributors[1]);

    }

}
