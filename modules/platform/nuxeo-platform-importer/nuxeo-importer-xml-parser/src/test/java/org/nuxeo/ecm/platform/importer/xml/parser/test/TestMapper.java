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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.importer.xml.parser.XMLImporterService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Test low level importer.
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("nuxeo-importer-xml-parser")
@Deploy("nuxeo-importer-xml-parser:test-ImporterMapping-mapper.xml")
public class TestMapper {

    @Inject
    protected XMLImporterService service;

    @Inject
    protected CoreSession session;

    @Test
    public void test() throws IOException {

        File xml = FileUtils.getResourceFileFromContext("depot.xml");
        assertNotNull(xml);

        DocumentModel root = session.getRootDocument();
        service.importDocuments(root, xml);
        session.save();

        List<DocumentModel> docs = session.query("SELECT * FROM Workspace");
        assertEquals("we should have only one Seance", 1, docs.size());
        DocumentModel seanceDoc = docs.get(0);

        docs = session.query(String.format("SELECT * FROM Document WHERE %s = '%s'", NXQL.ECM_PRIMARYTYPE, "Folder"));
        assertEquals("we should have 4 actes", 4, docs.size());

        docs = session.query(String.format("SELECT * FROM Document WHERE %s = '%s'", NXQL.ECM_PRIMARYTYPE, "File"));
        assertEquals("we should have 13 files", 13, docs.size());

        docs = session.query(String.format("SELECT * FROM Document WHERE %s = '%s' AND %s = '%s'", NXQL.ECM_PRIMARYTYPE,
                "Folder", NXQL.ECM_PARENTID, seanceDoc.getId()));
        assertEquals("we should have 3 actes in the seance", 3, docs.size());

        docs = session.query(String.format("SELECT * FROM Document WHERE %s = '%s' AND %s != '%s'",
                NXQL.ECM_PRIMARYTYPE, "Folder", NXQL.ECM_PARENTID, seanceDoc.getId()));
        assertEquals("we should have only 1 actes ouside of the seance", 1, docs.size());

        docs = session.query(String.format("SELECT * FROM Document WHERE %s = '%s' AND %s = '%s'", NXQL.ECM_PRIMARYTYPE,
                "File", NXQL.ECM_PARENTID, seanceDoc.getId()));
        assertEquals("we should have only 4 files in the seance", 4, docs.size());

        docs = session.query("SELECT * FROM Document ORDER BY " + NXQL.ECM_PATH);
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
