/*
 * (C) Copyright 2014-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.Serializable;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepositoryService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.StorageConfiguration;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.core.convert")
@Deploy("org.nuxeo.ecm.core.convert.plugins")
@Deploy("org.nuxeo.runtime.reload")
public class TestSQLBinariesIndexingOverride {

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected HotDeployer deployer;

    @Before
    public void setUp() throws Exception {
        assumeTrue(coreFeature.getStorageConfiguration().isVCS());
        // SQL Server fulltext indexes can't easily be updated by Nuxeo
        assumeTrue(!coreFeature.getStorageConfiguration().isVCSSQLServer());

        // cannot be done through @Deploy, because the framework variables
        // about repository configuration aren't ready yet
        deployer.deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-override-indexing-contrib.xml");
    }

    protected void waitForFulltextIndexing() {
        nextTransaction();
        coreFeature.getStorageConfiguration().waitForFulltextIndexing();
    }

    protected void nextTransaction() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    @Test
    public void testTwoBinaryIndexes() throws Exception {
        DocumentModelList res;
        DocumentModel doc = session.createDocumentModel("/", "source", "File");
        doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob("test"));
        doc = session.createDocument(doc);
        session.save();

        waitForFulltextIndexing();

        // main index
        res = session.query("SELECT * FROM Document WHERE ecm:fulltext = 'test'");
        assertEquals(1, res.size());

        // other index
        res = session.query("SELECT * FROM Document WHERE ecm:fulltext_binaries = 'test'");
        assertEquals(1, res.size());
    }

    @Test
    public void testGetBinaryFulltext() throws Exception {
        DocumentModelList res;
        DocumentModel doc = session.createDocumentModel("/", "source", "File");
        doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob("test"));
        doc = session.createDocument(doc);
        session.save();

        waitForFulltextIndexing();

        // main index
        res = session.query("SELECT * FROM Document WHERE ecm:fulltext = 'test'");
        assertEquals(1, res.size());
        Map<String, String> map = session.getBinaryFulltext(res.get(0).getRef());
        assertTrue(map.containsValue(" test "));
        StorageConfiguration database = coreFeature.getStorageConfiguration();
        if (!(database.isVCSMySQL() || database.isVCSSQLServer())) {
            // we have 2 binaries field
            assertTrue(map.containsKey("binarytext"));
            assertTrue(map.containsKey("binarytext_binaries"));
            assertEquals(" test ", map.get("binarytext"));
            assertEquals(" test ", map.get("binarytext_binaries"));
        }
    }

    @Test
    public void testExcludeFieldBlob() throws Exception {
        DocumentModelList res;
        DocumentModel doc = session.createDocumentModel("/", "source", "File");
        doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob("test"));
        doc = session.createDocument(doc);
        session.save();

        waitForFulltextIndexing();

        // indexes the skip file:content

        res = session.query("SELECT * FROM Document WHERE ecm:fulltext_nofile1 = 'test'");
        assertEquals(0, res.size());
        res = session.query("SELECT * FROM Document WHERE ecm:fulltext_nofile2 = 'test'");
        assertEquals(0, res.size());
        res = session.query("SELECT * FROM Document WHERE ecm:fulltext_nofile3 = 'test'");
        assertEquals(0, res.size());
        res = session.query("SELECT * FROM Document WHERE ecm:fulltext_nofile4 = 'test'");
        assertEquals(0, res.size());
    }

    @Test
    public void testEnforceFulltextFieldSizeLimit() throws Exception {
        SQLRepositoryService repositoryService = Framework.getService(SQLRepositoryService.class);
        RepositoryDescriptor repositoryDescriptor = repositoryService.getRepositoryImpl(
                session.getRepositoryName()).getRepositoryDescriptor();
        int fulltextFieldSizeLimit = repositoryDescriptor.getFulltextDescriptor().getFulltextFieldSizeLimit();
        assertEquals(1024, fulltextFieldSizeLimit); // from XML config

        String query = "SELECT * FROM Document WHERE ecm:fulltext = %s";
        DocumentModelList res;
        for (int i = 0; i < 2; i++) {
            boolean regular = i == 0;
            String name = regular ? "regContent" : "bigContent";
            String content = "";
            for (int j = 0; j < 93; j++) {
                content += "regContent" + " ";
            }
            if (!regular) {
                for (int j = 0; j < 50; j++) {
                    content += "bigContent" + " ";
                }
            }
            assertEquals(regular ? 1023 : 1573, content.length()); // > 1024 if not regular

            DocumentModel doc = session.createDocumentModel("/", name, "File");
            doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob(content));
            doc = session.createDocument(doc);
            session.save();

            waitForFulltextIndexing();

            // main index
            res = session.query(String.format(query, NXQL.escapeString(content)));
            if (regular) {
                assertEquals(1, res.size());
            } else {
                assertEquals(0, res.size());
                content = content.substring(0, fulltextFieldSizeLimit - 1);
                res = session.query(String.format(query, NXQL.escapeString(content)));
                assertEquals(2, res.size());
            }
        }
    }

}
