/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     annejubert
 */

package org.nuxeo.io.fsexporter.test;

import java.io.File;
import java.io.Serializable;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.io.fsexporter.FSExporter;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author annejubert
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy({ "nuxeo-fsexporter" })
public class TestFSExporterCase2 {

    @Inject
    CoreSession session;

    @Inject
    FSExporter service;

    @Test
    public void shouldExportFile() throws Exception {
        // creation of folders
        DocumentModel folder = session.createDocumentModel("/default-domain/", "myfolder1", "Folder");
        folder.setPropertyValue("dc:title", "Mon premier repertoire");
        session.createDocument(folder);

        DocumentModel folder2 = session.createDocumentModel("/default-domain/", "myfolder2", "Folder");
        folder.setPropertyValue("dc:title", "Mon deuxieme repertoire");
        session.createDocument(folder2);

        DocumentModel subFolder = session.createDocumentModel(folder2.getPathAsString(), "subFolder", "Folder");
        subFolder.setPropertyValue("dc:title", "Mon deuxieme repertoire");
        session.createDocument(subFolder);

        // creation of files
        DocumentModel file = session.createDocumentModel(folder.getPathAsString(), "myfile", "File");
        file.setPropertyValue("dc:title", "Mon premier fichier");

        Blob blob = new StringBlob("some content");
        blob.setFilename("MyFile.txt");
        blob.setMimeType("text/plain");
        file.setPropertyValue("file:content", (Serializable) blob);
        session.createDocument(file);

        DocumentModel file2 = session.createDocumentModel(folder2.getPathAsString(), "myfile2", "File");
        file2.setPropertyValue("dc:title", "Mon deuxieme fichier");

        Blob blob2 = new StringBlob("some content");
        blob2.setFilename("MyFile2.txt");
        blob2.setMimeType("text/plain");
        file2.setPropertyValue("file:content", (Serializable) blob2);
        session.createDocument(file2);

        DocumentModel fileSubFolder = session.createDocumentModel(subFolder.getPathAsString(), "fileSubFolder", "File");
        fileSubFolder.setPropertyValue("dc:title", "mon fichier dans un subfolder");

        Blob blobSubFolder = new StringBlob("some content");
        blobSubFolder.setFilename("MyFileSubFolder.txt");
        blobSubFolder.setMimeType("text/plain");
        fileSubFolder.setPropertyValue("file:content", (Serializable) blobSubFolder);
        session.createDocument(fileSubFolder);

        session.save();

        String tmp = Environment.getDefault().getTemp().getPath();
        service.export(session, "/default-domain/", tmp, "");

        String pathPrefix = StringUtils.removeEnd(tmp, "/");
        String targetPath = pathPrefix + folder.getPathAsString() + "/" + blob.getFilename();
        Assert.assertTrue("myfile.txt must exist", new File(targetPath).exists());
        String targetPathSubFolder = pathPrefix + subFolder.getPathAsString() + "/" + blobSubFolder.getFilename();
        Assert.assertTrue("MyFileSubFolder.txt must exist", new File(targetPathSubFolder).exists());
    }
}
