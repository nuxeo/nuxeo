/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Harlan Brown
 */
package org.nuxeo.ecm.platform.importer.xml.parser.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.importer.xml.parser.XMLImporterService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("nuxeo-importer-xml-parser")
@Deploy("nuxeo-importer-xml-parser:test-ImporterMapping3-contrib.xml")

public class TestDocUpdateWithDepotData {

    @Inject
    CoreSession session;

	@Test
	public void test() throws Exception {
        File xml = FileUtils.getResourceFileFromContext("depot3.xml");
        Assert.assertNotNull(xml);

        DocumentModel root = session.getRootDocument();

        XMLImporterService importer = Framework.getService(XMLImporterService.class);
        Assert.assertNotNull(importer);
        importer.importDocuments(root, xml);
        session.save();

        List<DocumentModel> docs = session.query("select * from Document where ecm:primaryType='File'");
        //without overwrite, 59-DELIB_0001.pdf and 37-DELIB_0003.pdf go in twice
        //Assert.assertEquals("we should have 12 files", 12, docs.size());
        Assert.assertEquals("we should have 10 files", 10, docs.size());

        DocumentRef ref = new PathRef("/Workspace-1/pv.pdf");
        DocumentModel doc = session.getDocument(ref);
        Assert.assertEquals("File pv.pdf should have 2 subjects", 2, ((String[]) doc.getPropertyValue("dc:subjects")).length);
        Assert.assertEquals("File pv.pdf should have 1 contributor", 1, ((String[]) doc.getPropertyValue("dc:contributors")).length);

        ref = new PathRef("/Workspace-1/odj.pdf");
        doc = session.getDocument(ref);
        Assert.assertNull("File odj.pdf should have 0 subjects", doc.getPropertyValue("dc:subjects"));
        Assert.assertEquals("File odj.pdf should have 2 Actors", 2, ((ArrayList<?>) doc.getPropertyValue("complx:Actors")).size());

        ref = new PathRef("/Workspace-1/pvcomplet.pdf");
        doc = session.getDocument(ref);
        Assert.assertEquals("File pvcomplet.pdf should have 5 subjects", 5, ((String[]) doc.getPropertyValue("dc:subjects")).length);
        Assert.assertEquals("File pvcomplet.pdf should have 3 contributors", 3, ((String[]) doc.getPropertyValue("dc:contributors")).length);

        ref = new PathRef("/Workspace-1/Acte-38/DECISION.pdf");
        doc = session.getDocument(ref);
        Assert.assertNull("File DECISION.pdf should have 0 subjects", doc.getPropertyValue("dc:subjects"));
        Assert.assertEquals("File DECISION.pdf should have 5 contributors", 5, ((String[]) doc.getPropertyValue("dc:contributors")).length);
        Boolean b = Arrays.asList((String[]) doc.getPropertyValue("dc:contributors")).contains("Waldo King");
        Assert.assertFalse("File DECISION.pdf should NOT contain a contributor named Waldo King",b);

        xml = FileUtils.getResourceFileFromContext("depot4.xml");
        Assert.assertNotNull(xml);
        importer.importDocuments(root, xml);
        session.save();

        docs = session.query("select * from Workspace");
        Assert.assertEquals("we should have only one Seance", 1, docs.size());
        DocumentModel seanceDoc = docs.get(0);

        docs = session.query("select * from Document where ecm:primaryType='Folder'");
        Assert.assertEquals("we should have 4 actes", 4, docs.size());

        docs = session.query("select * from Document where ecm:primaryType='File'");
        //without overwrite, 59-DELIB_0001.pdf and 37-DELIB_0003.pdf go in twice
        //Assert.assertEquals("we should have 12 files", 12, docs.size());
        Assert.assertEquals("we should have 10 files", 10, docs.size());

        ref = new PathRef("/Workspace-1/odj.pdf");
        doc = session.getDocument(ref);
        Assert.assertNull("File odj.pdf should have 0 subjects", doc.getPropertyValue("dc:subjects"));
        Assert.assertEquals("File odj.pdf should have 3 Actors", 3, ((ArrayList<?>) doc.getPropertyValue("complx:Actors")).size());

        ref = new PathRef("/Workspace-1/pv.pdf");
        doc = session.getDocument(ref);
        Assert.assertEquals("File pv.pdf should have 2 subjects", 2, ((String[]) doc.getPropertyValue("dc:subjects")).length);
        Assert.assertEquals("File pv.pdf should have 2 contributors", 2, ((String[]) doc.getPropertyValue("dc:contributors")).length);

        ref = new PathRef("/Workspace-1/pvcomplet.pdf");
        doc = session.getDocument(ref);
        Assert.assertNull("File pvcomplet.pdf should have 0 subjects", doc.getPropertyValue("dc:subjects"));
        Assert.assertEquals("File pvcomplet.pdf should have 2 contributors", 2, ((String[]) doc.getPropertyValue("dc:contributors")).length);
        b = Arrays.asList((String[]) doc.getPropertyValue("dc:contributors")).contains("Eddie Murphy");
        Assert.assertTrue("File pvcomplet.pdf should contain a contributor named Eddie Murphy",b);
        b = Arrays.asList((String[]) doc.getPropertyValue("dc:contributors")).contains("Andres Blackwood");
        Assert.assertFalse("File pvcomplet.pdf should NOT contain a contributor named Andres Blackwood",b);

        ref = new PathRef("/Workspace-1/Acte-38/DECISION.pdf");
        doc = session.getDocument(ref);
        Assert.assertNull("File DECISION.pdf should have 0 subjects", doc.getPropertyValue("dc:subjects"));
        Assert.assertEquals("File DECISION.pdf should have 6 contributors", 6, ((String[]) doc.getPropertyValue("dc:contributors")).length);
        b = Arrays.asList((String[]) doc.getPropertyValue("dc:contributors")).contains("Waldo King");
        Assert.assertTrue("File DECISION.pdf should contain a contributor named Waldo King",b);

        docs = session.query("select * from Document where ecm:primaryType='Section'");
        Assert.assertEquals("we should have 1 Section", 1, docs.size());

        docs = session.query("select * from Document where ecm:primaryType='Folder' AND ecm:parentId='"
                + seanceDoc.getId() + "'");
        Assert.assertEquals("we should have 3 actes in the seance", 3, docs.size());

        docs = session.query("select * from Document where ecm:primaryType='Folder' AND ecm:parentId!='"
                + seanceDoc.getId() + "'");
        Assert.assertEquals("we should have only 1 actes outside of the seance", 1, docs.size());

        docs = session.query("select * from Document where ecm:primaryType in ('File', 'Section')  AND ecm:parentId='"
                + seanceDoc.getId() + "'");
        Assert.assertEquals("we should have only 4 files in the seance", 4, docs.size());

	}

}
