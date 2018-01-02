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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
public class TestFSExporterAttachedFiles {

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

        // attached blob for myfile
        ArrayList<Map<String, Serializable>> listblobs = new ArrayList<>();

        Map<String, Serializable> mapBlob = new HashMap<>();
        Blob blob1 = new StringBlob("blob1");
        blob1.setFilename("blob1.txt");
        mapBlob.put("file", (Serializable) blob1);
        listblobs.add(mapBlob);

        Map<String, Serializable> mapBlob2 = new HashMap<>();
        Blob blob2 = new StringBlob("blob2");
        blob2.setFilename("blob2.txt");
        mapBlob2.put("file", (Serializable) blob2);
        listblobs.add(mapBlob2);

        // creation of myfile
        DocumentModel file = session.createDocumentModel(folder.getPathAsString(), "myfile", "File");
        file.setPropertyValue("dc:title", "Mon premier fichier");

        Blob blob = new StringBlob("some content");
        blob.setFilename("MyFileWithBlobs.txt");
        blob.setMimeType("text/plain");
        file.setPropertyValue("file:content", (Serializable) blob);
        file.setPropertyValue("files:files", listblobs);

        session.createDocument(file);
        session.save();

        String tmp = Environment.getDefault().getTemp().getPath();
        service.export(session, "/default-domain/", tmp, "");

        String pathPrefix = StringUtils.removeEnd(tmp, "/");
        String targetPath = pathPrefix + folder.getPathAsString() + "/" + blob.getFilename();
        Assert.assertTrue(new File(targetPath).exists());

        // The code has added the name as prefix: "myfile-"
        String targetPathBlob1 = pathPrefix + folder.getPathAsString() + "/myfile-" + blob1.getFilename();
        Assert.assertTrue("blob must exist", new File(targetPathBlob1).exists());
        String targetPathBlob2 = pathPrefix + folder.getPathAsString() + "/myfile-" + blob2.getFilename();
        Assert.assertTrue("blob must exist", new File(targetPathBlob2).exists());
    }
}
