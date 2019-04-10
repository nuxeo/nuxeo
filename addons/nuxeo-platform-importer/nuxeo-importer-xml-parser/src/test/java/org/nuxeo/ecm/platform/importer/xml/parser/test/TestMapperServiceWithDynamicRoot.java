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
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.importer.xml.parser.XMLImporterService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Test dynamic root creation
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("nuxeo-importer-xml-parser")
@Deploy("nuxeo-importer-xml-parser:test-ImporterMapping2-contrib.xml")
public class TestMapperServiceWithDynamicRoot {

    @Inject
    CoreSession session;

    @Inject
    XMLImporterService importerService;

    @Test
    public void test() throws Exception {

        File xml = FileUtils.getResourceFileFromContext("depot2.xml");
        Assert.assertNotNull(xml);

        DocumentModel root = session.getRootDocument();

        XMLImporterService importer = Framework.getService(XMLImporterService.class);
        Assert.assertNotNull(importer);
        importer.importDocuments(root, xml);

        session.save();

        List<DocumentModel> docs = session.getChildren(new PathRef("/2013/02/"));
        // check we have 2 children : i.e. parent path was merged ok
        Assert.assertEquals(2, docs.size());

        docs = session.getChildren(new PathRef("/2013/02/07/Workspace-4"));
        // check we have 2 children Acte
        Assert.assertEquals(2, docs.size());

        Assert.assertTrue(session.exists(new PathRef("/2013/02/08/Workspace-8/Acte-37")));
        Assert.assertTrue(session.exists(new PathRef("/Acte-149")));

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
