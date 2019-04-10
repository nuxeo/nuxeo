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

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.importer.xml.parser.XMLImporterServiceImpl;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Test low level importer (outside of service and extension point system)
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestMapper {

    @Inject
    CoreSession session;

    @Test
    public void test() throws Exception {

        File xml = FileUtils.getResourceFileFromContext("depot.xml");
        Assert.assertNotNull(xml);

        DocumentModel root = session.getRootDocument();

        XMLImporterServiceImpl parser = new XMLImporterServiceImpl(root, new DummyRegistry());

        parser.parse(xml);

        session.save();

        List<DocumentModel> docs = session.query("select * from Workspace");
        Assert.assertEquals("we should have only one Seance", 1, docs.size());
        DocumentModel seanceDoc = docs.get(0);

        docs = session.query("select * from Document where ecm:primaryType='Folder'");
        Assert.assertEquals("we should have 4 actes", 4, docs.size());

        docs = session.query("select * from Document where ecm:primaryType='File'");
        Assert.assertEquals("we should have 13 files", 13, docs.size());

        docs = session.query("select * from Document where ecm:primaryType='Folder' AND ecm:parentId='"
                + seanceDoc.getId() + "'");
        Assert.assertEquals("we should have 3 actes in the seance", 3, docs.size());

        docs = session.query("select * from Document where ecm:primaryType='Folder'  AND ecm:parentId!='"
                + seanceDoc.getId() + "'");
        Assert.assertEquals("we should have only 1 actes ouside of the seance", 1, docs.size());

        docs = session.query("select * from Document where ecm:primaryType='File'  AND ecm:parentId='"
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

}
