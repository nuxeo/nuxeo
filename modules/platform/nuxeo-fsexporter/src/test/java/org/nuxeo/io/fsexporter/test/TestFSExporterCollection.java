/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Charles Boidot
 */
package org.nuxeo.io.fsexporter.test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.Serializable;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.collections.core.test.CollectionFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.io.fsexporter.FSExporter;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CollectionFeature.class)
@Deploy("nuxeo-fsexporter")
public class TestFSExporterCollection {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder(new File(FeaturesRunner.getBuildDirectory()));

    @Inject
    protected CoreSession session;

    @Inject
    protected CollectionManager collectionManager;

    @Inject
    protected FSExporter service;

    @Test
    public void shouldExportCollection() throws Exception {

        String collectionName = "CollectionNameTest";
        String collectionDescription = "dummy";

        DocumentModel testWorkspace = session.createDocumentModel("/default-domain/workspaces", "testWorkspace",
                "Workspace");
        testWorkspace = session.createDocument(testWorkspace);

        DocumentModel testFile = session.createDocumentModel("", "testFileName", "File");
        Blob blobTestFile = Blobs.createBlob("some content", "text/plain", "UTF-8", "My_File_In_Section.txt");
        testFile.setPropertyValue("file:content", (Serializable) blobTestFile);
        testFile = session.createDocument(testFile);

        DocumentModel testFolder = session.createDocumentModel(testWorkspace.getPathAsString(), "testFolderName",
                "Folder");
        testFolder = session.createDocument(testFolder);

        DocumentModel testFolderFile = session.createDocumentModel(testFolder.getPathAsString(), "testFolderFileName",
                "File");
        Blob blobTestFolderFile = Blobs.createBlob("some content", "text/plain", "UTF-8", "My_File_In_Section.txt");
        testFolderFile.setPropertyValue("file:content", (Serializable) blobTestFolderFile);
        testFolderFile = session.createDocument(testFolderFile);

        collectionManager.addToNewCollection(collectionName, collectionDescription, testWorkspace, session);
        String collectionFullPath = collectionManager.getUserDefaultCollections(session).getPathAsString() + "/"
                + collectionName;

        assertTrue(session.exists(new PathRef(collectionFullPath)));
        DocumentModel collection = session.getDocument(new PathRef(collectionFullPath));

        collectionManager.addToCollection(collection, testFile, session);

        String tmp = folder.newFolder("fs-exporter").getAbsolutePath();
        service.export(session, collection.getPathAsString(), tmp, "");

        String pathPrefix = StringUtils.removeEnd(tmp, "/");

        String pathCollection = pathPrefix + "/" + collectionName;
        String pathWorkspace = pathCollection + "/" + testWorkspace.getName();
        String pathTestFile = pathCollection + "/" + blobTestFile.getFilename();
        String pathTestFolder = pathWorkspace + "/" + testFolder.getName();
        String pathTestFolderFile = pathTestFolder + "/" + blobTestFolderFile.getFilename();

        assertTrue("Collection must exist", new File(pathCollection).exists());
        assertTrue("Workspace must exist", new File(pathWorkspace).exists());
        assertTrue("TestFile must exist", new File(pathTestFile).exists());
        assertTrue("TestFolder must exist", new File(pathTestFolder).exists());
        assertTrue("TestFolderFile must exist", new File(pathTestFolderFile).exists());

    }

}
