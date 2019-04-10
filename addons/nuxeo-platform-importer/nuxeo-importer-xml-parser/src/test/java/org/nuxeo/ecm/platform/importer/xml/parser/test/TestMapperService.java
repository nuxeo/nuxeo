/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.platform.importer.xml.parser.test;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.importer.xml.parser.XMLImporterService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * Verify Service mapping
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("nuxeo-importer-xml-parser")
@LocalDeploy("nuxeo-importer-xml-parser:test-ImporterMapping-contrib.xml")
public class TestMapperService {

    @Inject
    CoreSession session;

    @Inject
    XMLImporterService importerService;

    @Test
    public void test() throws Exception {

        File xml = FileUtils.getResourceFileFromContext("depot.xml");
        Assert.assertNotNull(xml);

        DocumentModel root = session.getRootDocument();

        XMLImporterService importer = Framework.getService(XMLImporterService.class);
        Assert.assertNotNull(importer);
        importer.importDocuments(root, xml);

        session.save();

        List<DocumentModel> docs = session.query("select * from Workspace");
        Assert.assertEquals("we should have only one Seance and one ", 1, docs.size());
        DocumentModel seanceDoc = docs.get(0);

        docs = session.query("select * from Document where ecm:primaryType='Folder'");
        Assert.assertEquals("we should have 4 actes", 4, docs.size());

        docs = session.query("select * from Document where ecm:primaryType='File'");
        Assert.assertEquals("we should have 12 files", 12, docs.size());

        docs = session.query("select * from Document where ecm:primaryType='Section'");
        Assert.assertEquals("we should have 1 Section", 1, docs.size());

        docs = session.query("select * from Document where ecm:primaryType='Folder' AND ecm:parentId='"
                + seanceDoc.getId() + "'");
        Assert.assertEquals("we should have 3 actes in the seance", 3, docs.size());

        docs = session.query("select * from Document where ecm:primaryType='Folder'  AND ecm:parentId!='"
                + seanceDoc.getId() + "'");
        Assert.assertEquals("we should have only 1 actes outside of the seance", 1, docs.size());

        docs = session.query("select * from Document where ecm:primaryType in ('File', 'Section')  AND ecm:parentId='"
                + seanceDoc.getId() + "'");
        Assert.assertEquals("we should have only 4 files in the seance", 4, docs.size());

        docs = session.query("select * from Document order by ecm:path");
        for (DocumentModel doc : docs) {
            if (!doc.getId().equals(root.getId())) {
                System.out.println("> [" + doc.getType() + "] " + doc.getPathAsString() + " : " + " - title: '"
                        + doc.getTitle() + "', dc:source: '" + doc.getPropertyValue("dc:source"));
                BlobHolder bh = doc.getAdapter(BlobHolder.class);
                if (bh != null) {
                    Blob blob = bh.getBlob();
                    if (blob != null) {
                        System.out.println(" ------ > File " + blob.getFilename() + " " + blob.getMimeType() + " "
                                + blob.getLength());
                    }
                }
            }
        }
    }

    @Test
    public void testNXP11834() throws Exception {

        File xml = FileUtils.getResourceFileFromContext("NXP-11834.xml");
        Assert.assertNotNull(xml);

        DocumentModel root = session.getRootDocument();

        XMLImporterService importer = Framework.getService(XMLImporterService.class);
        Assert.assertNotNull(importer);
        try {
            importer.importDocuments(root, xml);
        } catch (ClassCastException e) {
            fail("See NXP-11834 ticket");

        }
    }

    @Test
    public void testZip() throws Exception {

        File zipXml = FileUtils.getResourceFileFromContext("nuxeo-webdelib-export.zip");
        Assert.assertNotNull(zipXml);

        DocumentModel root = session.getRootDocument();

        XMLImporterService importer = Framework.getService(XMLImporterService.class);
        Assert.assertNotNull(importer);
        importer.importDocuments(root, zipXml);

        session.save();

        List<DocumentModel> docs = session.query("select * from Workspace");
        Assert.assertEquals("we should have only one Seance and one ", 1, docs.size());
        DocumentModel seanceDoc = docs.get(0);

        docs = session.query("select * from Document where ecm:primaryType='Folder'");
        Assert.assertEquals("we should have 5 actes", 5, docs.size());

        docs = session.query("select * from Document where ecm:primaryType in ('File','Section')");
        Assert.assertEquals("we should have 18 files", 18, docs.size());

        docs = session.query("select * from Document where ecm:primaryType='Folder' AND ecm:parentId='"
                + seanceDoc.getId() + "'");
        Assert.assertEquals("we should have 4 actes in the seance", 4, docs.size());

        docs = session.query("select * from Document where ecm:primaryType='Folder'  AND ecm:parentId!='"
                + seanceDoc.getId() + "'");
        Assert.assertEquals("we should have only 1 actes outside of the seance", 1, docs.size());

        docs = session.query("select * from Document where ecm:primaryType in ('File', 'Section')  AND ecm:parentId='"
                + seanceDoc.getId() + "'");
        Assert.assertEquals("we should have only 4 files in the seance", 4, docs.size());

        IterableQueryResult result = session.queryAndFetch("select content/name from File", "NXQL", (Object[]) null);

        for (Iterator<Map<String, Serializable>> rows = result.iterator(); rows.hasNext();) {
            String filename = (String) rows.next().get("content/name");
            Assert.assertTrue(filename.endsWith(".pdf") || filename.endsWith(".zip") || filename.endsWith(".pdf2"));
        }
        docs = session.query("select * from Document where ecm:primaryType in ('File')");
        for (DocumentModel fileDoc : docs) {
            Blob blob = fileDoc.getAdapter(BlobHolder.class).getBlob();
            Assert.assertNotNull(blob);
            Assert.assertNotNull(blob.getFilename());
            Assert.assertNotNull(blob.getMimeType());
            Assert.assertTrue(blob.getFilename().endsWith(".pdf2") || blob.getLength() > 1000);
        }

        docs = session.query("select * from Document order by ecm:path");
        for (DocumentModel doc : docs) {
            if (!doc.getId().equals(root.getId())) {
                System.out.println("> [" + doc.getType() + "] " + doc.getPathAsString() + " : " + " - title: '"
                        + doc.getTitle() + "', dc:source: '" + doc.getPropertyValue("dc:source"));
                BlobHolder bh = doc.getAdapter(BlobHolder.class);
                if (bh != null) {
                    Blob blob = bh.getBlob();
                    if (blob != null) {
                        System.out.println(" ------ > File " + blob.getFilename() + " " + blob.getMimeType() + " "
                                + blob.getLength());
                    }
                }
            }
        }
    }

}
