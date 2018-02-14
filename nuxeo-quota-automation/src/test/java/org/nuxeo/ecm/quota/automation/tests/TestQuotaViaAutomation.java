/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */
package org.nuxeo.ecm.quota.automation.tests;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.quota.QuotaStatsService;
import org.nuxeo.ecm.quota.automation.GetQuotaInfoOperation;
import org.nuxeo.ecm.quota.automation.SetQuotaInfoOperation;
import org.nuxeo.ecm.quota.automation.SimpleQuotaInfo;
import org.nuxeo.ecm.quota.automation.TestableJsonAdapter;
import org.nuxeo.ecm.quota.size.QuotaAware;
import org.nuxeo.ecm.quota.size.QuotaAwareDocumentFactory;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 5.6
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.quota.core")
@Deploy("org.nuxeo.ecm.quota.automation")
@Deploy("org.nuxeo.ecm.automation.core")
public class TestQuotaViaAutomation {

    @Inject
    AutomationService automationService;

    @Inject
    protected QuotaStatsService quotaStatsService;

    @Inject
    protected CoreSession session;

    @Inject
    protected EventService eventService;

    @Inject
    protected FeaturesRunner featureRunner;

    @Inject
    protected RuntimeHarness harness;

    protected DocumentRef wsRef;

    protected DocumentRef firstFolderRef;

    protected DocumentRef secondFolderRef;

    protected DocumentRef firstSubFolderRef;

    protected DocumentRef secondSubFolderRef;

    protected DocumentRef firstFileRef;

    protected DocumentRef secondFileRef;

    protected static final boolean verboseMode = false;

    @Before
    public void cleanupSessionAssociationBeforeTest() throws Exception {
        // temp fix to be sure the session tx
        // will be correctly handled in the test
    }

    protected Blob getFakeBlob(int size) {
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append('a');
        }
        Blob blob = Blobs.createBlob(sb.toString());
        blob.setFilename("FakeBlob_" + size + ".txt");
        return blob;
    }

    protected void addContent() throws Exception {
        TransactionHelper.startTransaction();

        try {
            DocumentModel ws = session.createDocumentModel("/", "ws", "Workspace");
            ws = session.createDocument(ws);
            wsRef = ws.getRef();

            QuotaAware wsqa = QuotaAwareDocumentFactory.make(ws);
            wsqa.setMaxQuota(400);
            wsqa.save();

            DocumentModel firstFolder = session.createDocumentModel(ws.getPathAsString(), "folder1", "Folder");
            firstFolder = session.createDocument(firstFolder);
            firstFolderRef = firstFolder.getRef();

            DocumentModel firstSubFolder = session.createDocumentModel(firstFolder.getPathAsString(), "subfolder1",
                    "Folder");
            firstSubFolder = session.createDocument(firstSubFolder);

            firstSubFolderRef = firstSubFolder.getRef();

            DocumentModel firstFile = session.createDocumentModel(firstSubFolder.getPathAsString(), "file1", "File");
            firstFile.setPropertyValue("file:content", (Serializable) getFakeBlob(100));
            firstFile = session.createDocument(firstFile);

            firstFileRef = firstFile.getRef();

            DocumentModel secondFile = session.createDocumentModel(firstSubFolder.getPathAsString(), "file2", "File");
            secondFile.setPropertyValue("file:content", (Serializable) getFakeBlob(200));

            secondFile = session.createDocument(secondFile);
            secondFileRef = secondFile.getRef();

            DocumentModel secondSubFolder = session.createDocumentModel(firstFolder.getPathAsString(), "subfolder2",
                    "Folder");
            secondSubFolder = session.createDocument(secondSubFolder);
            secondSubFolderRef = secondSubFolder.getRef();

            DocumentModel secondFolder = session.createDocumentModel(ws.getPathAsString(), "folder2", "Folder");
            secondFolder = session.createDocument(secondFolder);
            secondFolderRef = secondFolder.getRef();

        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
        eventService.waitForAsyncCompletion();
        TransactionHelper.startTransaction();
    }

    protected void assertQuota(SimpleQuotaInfo sqi, long innerSize, long totalSize) {
        assertEquals(innerSize, sqi.getInnerSize());
        assertEquals(totalSize, sqi.getTotalSize());
    }

    protected SimpleQuotaInfo getQuotaInfo(DocumentRef docRef) throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(docRef);

        OperationChain chain = new OperationChain("fakeChain");
        chain.add(GetQuotaInfoOperation.ID);
        TestableJsonAdapter adapter = (TestableJsonAdapter) automationService.run(ctx, chain);
        return (SimpleQuotaInfo) adapter.getObject();
    }

    protected SimpleQuotaInfo getQuotaInfoViaParameter(DocumentRef docRef) throws Exception {
        OperationContext ctx = new OperationContext(session);

        OperationChain chain = new OperationChain("fakeChain");
        chain.add(GetQuotaInfoOperation.ID).set("documentRef", docRef);
        TestableJsonAdapter adapter = (TestableJsonAdapter) automationService.run(ctx, chain);
        return (SimpleQuotaInfo) adapter.getObject();
    }

    protected Long setQuota(DocumentRef docRef, long size) throws Exception {
        try (OperationContext ctx = new OperationContext(session)) {
            OperationChain chain = new OperationChain("fakeChain");
            chain.add(SetQuotaInfoOperation.ID).set("documentRef", docRef).set("targetSize", size);

            return (Long) automationService.run(ctx, chain);
        }
    }

    @Test
    public void testGetQuotasViaAutomation() throws Exception {

        addContent();
        TransactionHelper.startTransaction();
        try {
            SimpleQuotaInfo sqi_firstFile = getQuotaInfo(firstFileRef);
            SimpleQuotaInfo sqi_secondFile = getQuotaInfo(secondFileRef);
            SimpleQuotaInfo sqi_firstSubFolder = getQuotaInfo(firstSubFolderRef);
            SimpleQuotaInfo sqi_firstFolder = getQuotaInfo(firstFolderRef);
            SimpleQuotaInfo sqi_ws = getQuotaInfo(wsRef);

            assertQuota(sqi_firstFile, 100L, 100L);
            assertQuota(sqi_secondFile, 200L, 200L);
            assertQuota(sqi_firstSubFolder, 0L, 300L);
            assertQuota(sqi_firstFolder, 0L, 300L);
            assertQuota(sqi_ws, 0L, 300L);

            assertEquals(400L, sqi_ws.getMaxQuota());

            sqi_firstFile = getQuotaInfoViaParameter(firstFileRef);
            DocumentModel docFile = session.getDocument(firstFileRef);
            assertEquals(100L, docFile.getPropertyValue("dss:innerSize"));
            assertQuota(sqi_firstFile, 100L, 100L);
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
        eventService.waitForAsyncCompletion();
        TransactionHelper.startTransaction();
    }

    @Test
    public void testSetQuotasViaAutomation() throws Exception {
        addContent();

        SimpleQuotaInfo sqi_ws = getQuotaInfo(wsRef);
        assertQuota(sqi_ws, 0L, 300L);
        assertEquals(400L, sqi_ws.getMaxQuota());

        Long newSize = setQuota(wsRef, 500L);
        assertEquals(500L, newSize.longValue());
        sqi_ws = getQuotaInfo(wsRef);
        assertEquals(500L, sqi_ws.getMaxQuota());
    }

}
