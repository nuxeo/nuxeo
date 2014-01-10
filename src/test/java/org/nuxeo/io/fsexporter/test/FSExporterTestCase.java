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
public class FSExporterTestCase {

    @Inject
    CoreSession session;

    @Inject
    FSExporter service;

    @Test
    public void shouldExportFile() throws Exception {

        DocumentModel folder = session.createDocumentModel("/default-domain/",
                "myfolder", "Folder");
        folder.setPropertyValue("dc:title", "Mon premier repertoire");
        session.createDocument(folder);

        DocumentModel file = session.createDocumentModel(
                folder.getPathAsString(), "myfile", "File");
        file.setPropertyValue("dc:title", "Mon premier fichier");

        Blob blob = new StringBlob("some content");
        blob.setFilename("MyFile.txt");
        blob.setMimeType("text/plain");
        file.setPropertyValue("file:content", (Serializable) blob);
        session.createDocument(file);

        session.save();

        Framework.getLocalService(FSExporter.class);
        service.export(session, "/default-domain/", "/tmp/", "GET_CHILDREN_PP");

        // Assure que cela s'est passe comme attendu
        String targetPath = "/tmp" + folder.getPathAsString() + "/"
                + blob.getFilename();
        Assert.assertTrue(new File(targetPath).exists());
    }
}