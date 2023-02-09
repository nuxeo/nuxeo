/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume RENARD
 */

package org.nuxeo.ecm.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;

/**
 * @since 2021.32
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestRetainedProperties {

    @Inject
    protected HotDeployer deployer;

    @Inject
    protected CoreSession session;

    protected void assertSaveFail(DocumentModel doc) {
        var exception = assertThrows(DocumentSecurityException.class, () -> session.saveDocument(doc));
        assertEquals(String.format("Cannot change blob from document %s, it is under retention / hold", doc.getRef()),
                exception.getMessage());
    }

    protected DocumentModel createFileDocument(String name) {
        DocumentModel documentModel = session.createDocumentModel("/", name, "File");
        Blob blob = Blobs.createBlob(name);
        documentModel.setPropertyValue("file:content", (Serializable) blob);
        blob = Blobs.createBlob(name + name, "text/plain");
        documentModel.setPropertyValue("files:files",
                (Serializable) Collections.singletonList(Collections.singletonMap("file", blob)));
        return session.createDocument(documentModel);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:test-retain-files-property.xml")
    public void iHaveAccessToRetainedProperties() throws Exception {
        DocumentModel record = createFileDocument("record");
        assertNotNull(record.getRetainedProperties());
        assertTrue(record.getRetainedProperties().isEmpty());
        session.makeRecord(record.getRef());
        record.refresh();
        assertNotNull(record.getRetainedProperties());
        assertEquals(2, record.getRetainedProperties().size());
        assertEquals(session.getRetainedProperties(record.getRef()), record.getRetainedProperties());
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:test-retain-files-property.xml")
    public void iCannotRetainUnexistingProperties() throws Exception {
        // Create a folder that does not have file nor files schema
        DocumentModel record = session.createDocumentModel("/", "folder", "Folder");
        record = session.createDocument(record);
        assertNotNull(record.getRetainedProperties());
        assertTrue(record.getRetainedProperties().isEmpty());
        session.makeRecord(record.getRef());
        record.refresh();
        assertNotNull(record.getRetainedProperties());
        assertTrue(record.getRetainedProperties().isEmpty());
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:test-retain-files-property.xml")
    public void iCannotAlterAlreadyDefinedRetainedProperties() throws Exception {
        DocumentModel record = createFileDocument("record");
        session.makeRecord(record.getRef());
        record.refresh();
        assertNotNull(record.getRetainedProperties());
        assertEquals(2, record.getRetainedProperties().size());
        assertTrue(record.getRetainedProperties().contains("content"));
        assertTrue(record.getRetainedProperties().contains("files/*/file"));

        deployer.undeploy("org.nuxeo.ecm.core.test.tests:test-retain-files-property.xml");

        DocumentModel newDoc = createFileDocument("otherRecord");
        session.makeRecord(newDoc.getRef());
        List<String> newDocRetainedProps = session.getRetainedProperties(newDoc.getRef());
        assertNotNull(newDocRetainedProps);
        assertEquals(1, newDocRetainedProps.size());
        assertTrue(newDocRetainedProps.contains("content"));

        assertNotNull(record.getRetainedProperties());
        assertEquals(2, record.getRetainedProperties().size());
        assertTrue(record.getRetainedProperties().contains("content"));
        assertTrue(record.getRetainedProperties().contains("files/*/file"));
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:test-retain-files-property.xml")
    public void iCannotEditAlreadyDefinedRetainedProperties() throws Exception {
        DocumentModel record = createFileDocument("record");
        assertNotNull(String.format("Property files:files must not be null."), record.getPropertyValue("files:files"));
        session.makeRecord(record.getRef());
        session.setLegalHold(record.getRef(), true, null);
        record.refresh();

        record.setPropertyValue("files:files", null);
        assertSaveFail(record);

        deployer.undeploy("org.nuxeo.ecm.core.test.tests:test-retain-files-property.xml");

        record.setPropertyValue("files:files", null);
        assertSaveFail(record);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:test-retain-blobList-property.xml")
    public void iCanRetainCustomBlobList() {
        DocumentModel doc = session.createDocumentModel("/", "bl", "BlobList");
        doc.setPropertyValue("blobList:myListOfBlob",
                (Serializable) Collections.singletonList(Blobs.createBlob("foo")));
        doc = session.createDocument(doc);
        session.makeRecord(doc.getRef());
        session.setLegalHold(doc.getRef(), true, null);
        assertNotNull(doc.getRetainedProperties());
        assertEquals(1, doc.getRetainedProperties().size());

        doc.setPropertyValue("blobList:myListOfBlob/0", (Serializable) Blobs.createBlob("bar"));
        assertSaveFail(doc);
    }

}
