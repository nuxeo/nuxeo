/*
 * (C) Copyright 2002-2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
