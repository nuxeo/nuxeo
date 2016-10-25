/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.StorageConfiguration;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.reload.ReloadService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.core.convert", //
        "org.nuxeo.ecm.core.convert.plugins", //
        "org.nuxeo.runtime.reload", //
})
public class TestSQLBinariesIndexingOverride {

    @Inject
    protected RuntimeHarness runtimeHarness;

    @Inject
    protected EventService eventService;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected ReloadService reloadService;

    protected boolean deployed;

    @Before
    public void setUp() throws Exception {
        assumeTrue(coreFeature.getStorageConfiguration().isVCS());
        // SQL Server fulltext indexes can't easily be updated by Nuxeo
        assumeTrue(!coreFeature.getStorageConfiguration().isVCSSQLServer());

        // cannot be done through @LocalDeploy, because the framework variables
        // about repository configuration aren't ready yet
        runtimeHarness.deployContrib("org.nuxeo.ecm.core.test.tests", "OSGI-INF/test-override-indexing-contrib.xml");
        deployed = true;
        newRepository(); // fully reread repo and its indexing config
    }

    @After
    public void tearDown() throws Exception {
        if (deployed) {
            runtimeHarness.undeployContrib("org.nuxeo.ecm.core.test.tests",
                    "OSGI-INF/test-override-indexing-contrib.xml");
            deployed = false;
        }
    }

    protected void newRepository() {
        waitForAsyncCompletion();
        coreFeature.releaseCoreSession();
        // reload repo with new config
        reloadService.reloadRepository();
        session = coreFeature.createCoreSession();
    }

    protected void waitForAsyncCompletion() {
        nextTransaction();
        eventService.waitForAsyncCompletion();
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
        assertTrue(map.containsValue("test"));
        StorageConfiguration database = coreFeature.getStorageConfiguration();
        if (!(database.isVCSMySQL() || database.isVCSSQLServer())) {
            // we have 2 binaries field
            assertTrue(map.containsKey("binarytext"));
            assertTrue(map.containsKey("binarytext_binaries"));
            assertEquals("test", map.get("binarytext"));
            assertEquals("test", map.get("binarytext_binaries"));
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
        DocumentModelList res;
        for (int i = 0; i < 2; i++) {
            String namePrefix = i == 0 ? "reg" : "big";
            String name = namePrefix + "Content";
            // fulltextFieldSizeLimit configured as 1024
            String content = name + " ";
            for (int j = 0; j < (i * 100) + 50; j++) {
                content += name + " ";
            }
            DocumentModel doc = session.createDocumentModel("/", name, "File");
            doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob(content));
            doc = session.createDocument(doc);
            session.save();

            waitForFulltextIndexing();

            // main index
            res = session.query("SELECT * FROM Document WHERE ecm:fulltext = '" + content + "'");
            assertEquals(i == 0 ? 1 : 0, res.size());
        }
    }

}
