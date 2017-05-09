/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.directory.test.io;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.io.registry.MarshallerHelper;
import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.Reader;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryEntry;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @since 9.2
 */
@RunWith(FeaturesRunner.class)
@Features({CoreFeature.class, DirectoryFeature.class})
@Deploy({ "org.nuxeo.ecm.directory", "org.nuxeo.ecm.core.io" })
@LocalDeploy("org.nuxeo.ecm.directory.resolver.test:test-directory-resolver-contrib.xml")
public class DocumentFetchJsonDirectoryEntryTest {

    @Inject
    private DirectoryService directoryService;

    @Inject
    private MarshallerRegistry marshallerRegistry;

    @Inject
    private CoreSession coreSession;

    @Test
    public void testReadScalar() throws Exception {
        DocumentModel docInput = coreSession.createDocumentModel("/", "test", "DirectoryReferencer");
        docInput.setPropertyValue("dr:directory1Ref", "123");
        docInput = coreSession.createDocument(docInput);
        String jsonDoc = MarshallerHelper.objectToJson(docInput, CtxBuilder.properties("*").get());

        String directoryName = "referencedDirectory1";
        String jsonDirectory;
        Directory directory = directoryService.getDirectory(directoryName);
        try (Session session = directory.getSession()) {
            DocumentModel entryDoc = session.getEntry("234");
            DirectoryEntry entry = new DirectoryEntry(directoryName, entryDoc);
            jsonDirectory = MarshallerHelper.objectToJson(entry, CtxBuilder.get());
        }

        // replace in the json
        String newJsonDoc = jsonDoc.replace("\"123\"", jsonDirectory);

        // update the doc
        Reader<DocumentModel> reader = marshallerRegistry.getReader(CtxBuilder.session(coreSession).get(),
                DocumentModel.class, APPLICATION_JSON_TYPE);
        ByteArrayInputStream input = new ByteArrayInputStream(newJsonDoc.getBytes());
        DocumentModel docUpdate = reader.read(DocumentModel.class, DocumentModel.class, APPLICATION_JSON_TYPE, input);
        coreSession.saveDocument(docUpdate);

        // test result
        DocumentModel docTest = coreSession.getDocument(new PathRef("/test"));
        assertEquals("234", docTest.getPropertyValue("dr:directory1Ref"));
    }

    @Test
    public void testArrayProperty() throws Exception {
        DocumentModel docInput = coreSession.createDocumentModel("/", "test", "DirectoryReferencer");
        docInput.setPropertyValue("dr:directory3Refs", new String[] { "123", "234" });
        docInput = coreSession.createDocument(docInput);
        String jsonDoc = MarshallerHelper.objectToJson(docInput, CtxBuilder.properties("*").get());

        String directoryName = "referencedDirectory1";
        String jsonDirectory;
        Directory directory = directoryService.getDirectory(directoryName);
        try (Session session = directory.getSession()) {
            DocumentModel entryDoc = session.getEntry("345");
            DirectoryEntry entry = new DirectoryEntry(directoryName, entryDoc);
            jsonDirectory = MarshallerHelper.objectToJson(entry, CtxBuilder.get());
        }

        // replace in the json
        String newJsonDoc = jsonDoc.replace("\"123\"", jsonDirectory);

        // update the doc
        Reader<DocumentModel> reader = marshallerRegistry.getReader(CtxBuilder.session(coreSession).get(),
                DocumentModel.class, APPLICATION_JSON_TYPE);
        ByteArrayInputStream input = new ByteArrayInputStream(newJsonDoc.getBytes());
        DocumentModel docUpdate = reader.read(DocumentModel.class, DocumentModel.class, APPLICATION_JSON_TYPE, input);
        coreSession.saveDocument(docUpdate);

        // test result
        DocumentModel docTest = coreSession.getDocument(new PathRef("/test"));
        List<String> values = Arrays.asList((String[]) docTest.getPropertyValue("dr:directory3Refs"));
        assertEquals(2, values.size());
        assertTrue(values.contains("234"));
        assertTrue(values.contains("345"));
    }

}
