/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     annejubert
 */

package org.nuxeo.io.fsexporter.test;

import java.io.File;
import java.io.Serializable;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.io.fsexporter.FSExporter;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 *
 * @author annejubert
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy({ "nuxeo-fsexporter" })
public class FSExporterTestCase2 {

    @Inject
    CoreSession session;

    @Inject
    FSExporter service;

    @Test
    public void shouldExportFile() throws Exception {

        // creation of folders
        DocumentModel folder = session.createDocumentModel("/default-domain/",
                "myfolder1", "Folder");
        folder.setPropertyValue("dc:title", "Mon premier repertoire");
        session.createDocument(folder);

        DocumentModel folder2 = session.createDocumentModel("/default-domain/",
                "myfolder2", "Folder");
        folder.setPropertyValue("dc:title", "Mon deuxieme repertoire");
        session.createDocument(folder2);

        DocumentModel subFolder = session.createDocumentModel(
                folder2.getPathAsString(), "subFolder", "Folder");
        subFolder.setPropertyValue("dc:title", "Mon deuxieme repertoire");
        session.createDocument(subFolder);

        // creation of files
        DocumentModel file = session.createDocumentModel(
                folder.getPathAsString(), "myfile", "File");
        file.setPropertyValue("dc:title", "Mon premier fichier");

        Blob blob = new StringBlob("some content");
        blob.setFilename("MyFile.txt");
        blob.setMimeType("text/plain");
        file.setPropertyValue("file:content", (Serializable) blob);
        session.createDocument(file);

        DocumentModel file2 = session.createDocumentModel(
                folder2.getPathAsString(), "myfile2", "File");
        file2.setPropertyValue("dc:title", "Mon deuxieme fichier");

        Blob blob2 = new StringBlob("some content");
        blob2.setFilename("MyFile2.txt");
        blob2.setMimeType("text/plain");
        file2.setPropertyValue("file:content", (Serializable) blob2);
        session.createDocument(file2);

        DocumentModel fileSubFolder = session.createDocumentModel(
                subFolder.getPathAsString(), "fileSubFolder", "File");
        fileSubFolder.setPropertyValue("dc:title",
                "mon fichier dans un subfolder");

        Blob blobSubFolder = new StringBlob("some content");
        blobSubFolder.setFilename("MyFileSubFolder.txt");
        blobSubFolder.setMimeType("text/plain");
        fileSubFolder.setPropertyValue("file:content",
                (Serializable) blobSubFolder);
        session.createDocument(fileSubFolder);

        session.save();

        Framework.getLocalService(FSExporter.class);
        service.export(session, "/default-domain/", "/tmp/", "GET_CHILDREN_PP");

        // verify that myfile.txt exists
        String targetPath = "/tmp" + folder.getPathAsString() + "/"
                + blob.getFilename();
        Assert.assertTrue(new File(targetPath).exists());

        // verify that MyFileSubFolder.txt exists
        String targetPathSubFolder = "/tmp" + subFolder.getPathAsString() + "/"
                + blobSubFolder.getFilename();
        Assert.assertTrue(new File(targetPathSubFolder).exists());

        // verify that

    }
}
