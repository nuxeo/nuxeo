/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     anechaev
 */
package org.nuxeo.ecm.platform.importer.xml.parser.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.importer.xml.parser.XMLImporterService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Verifying Service mapping with multiple file attachments
 *
 * @author <a href="mailto:anechaev@nuxeo.com">Mika</a>
 */

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("nuxeo-importer-xml-parser")
@LocalDeploy("nuxeo-importer-xml-parser:test-ImporterMapping-Multi-files-contrib.xml")
public class TestMapperServiceMultiFiles {

    @Inject
    private CoreSession session;

    @Test
    @SuppressWarnings("unchecked")
    public void shouldImportMultifilesAttachments() throws Exception {
        File xml = FileUtils.getResourceFileFromContext("multifiles.xml");
        assertNotNull(xml);

        DocumentModel root = session.getRootDocument();

        XMLImporterService importer = Framework.getService(XMLImporterService.class);
        assertNotNull(importer);
        importer.importDocuments(root, xml);

        session.save();

        List<DocumentModel> docs = session.query("SELECT * FROM Document WHERE dc:title='Multifile'");
        assertEquals("we should have only one File", 1, docs.size());

        DocumentModel doc = docs.get(0);

        Blob mainFile = (Blob) doc.getPropertyValue("file:content");
        BufferedReader reader = new BufferedReader(new InputStreamReader(mainFile.getStream()));
        String result = reader.readLine();
        assertEquals("file1", result);
        assertEquals("file1.txt", mainFile.getFilename());

        List<Map<String, Blob>> attachments = (List<Map<String, Blob>>) doc.getPropertyValue("files:files");
        assertEquals(2, attachments.size());

        assertEquals(2, attachments.size());
        Blob blob2 = attachments.get(0).get("file");
        assertEquals("file2.txt", blob2.getFilename());
        assertEquals("file2", blob2.getString());
        Blob blob3 = attachments.get(1).get("file");
        assertEquals("file3.txt", blob3.getFilename());
        assertEquals("file3", blob3.getString());
    }

}