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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
public class FSExporterTestAttachedFiles {

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

        // attached blob for myfile
        ArrayList<Map<String, Serializable>> listblobs = new ArrayList<Map<String, Serializable>>();

        Map<String, Serializable> mapBlob = new HashMap<String, Serializable>();
        Blob blob1 = new StringBlob("blob1");
        blob1.setFilename("blob1.txt");
        mapBlob.put("file", (Serializable) blob1);
        mapBlob.put("filename", "blob1.txt");
        listblobs.add(mapBlob);

        Map<String, Serializable> mapBlob2 = new HashMap<String, Serializable>();
        Blob blob2 = new StringBlob("blob2");
        blob2.setFilename("blob2.txt");
        mapBlob2.put("file", (Serializable) blob2);
        mapBlob2.put("filename", "blob2.txt");
        listblobs.add(mapBlob2);

        // creation of myfile
        DocumentModel file = session.createDocumentModel(
                folder.getPathAsString(), "myfile", "File");
        file.setPropertyValue("dc:title", "Mon premier fichier");

        Blob blob = new StringBlob("some content");
        blob.setFilename("MyFileWithBlobs.txt");
        blob.setMimeType("text/plain");
        file.setPropertyValue("file:content", (Serializable) blob);
        file.setPropertyValue("files:files", listblobs);

        session.createDocument(file);

        session.save();

        Framework.getLocalService(FSExporter.class);
        service.export(session, "/default-domain/", "/tmp/", "GET_CHILDREN_PP");

        String targetPath = "/tmp" + folder.getPathAsString() + "/"
                + blob.getFilename();
        Assert.assertTrue(new File(targetPath).exists());

        // verify that the blobs exist
        String targetPathBlob1 = "/tmp" + folder.getPathAsString() + "/"
                + blob1.getFilename();
        Assert.assertTrue(new File(targetPathBlob1).exists());
        String targetPathBlob2 = "/tmp" + folder.getPathAsString() + "/"
                + blob2.getFilename();
        Assert.assertTrue(new File(targetPathBlob2).exists());
    }
}
