/**
 * 
 */

package org.nuxeo.io.fsexporter.test;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
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

        /*
         * DocumentModelList docs
         * =session.query("select * from Document order by ecm:path");
         * 
         * for (DocumentModel doc : docs) { System.out.println(doc.toString());
         * }
         */

        String tmp = System.getProperty("java.io.tmp");

        Framework.getLocalService(FSExporter.class);
        service.export(session, "/default-domain/", "/tmp/", true);

        // Assure que cela s'est passe comme attendu
        String targetPath = "/tmp" + folder.getPathAsString() + "/"
                + blob.getFilename();
        Assert.assertTrue(new File(targetPath).exists());
    }
}