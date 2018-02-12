/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.quota.count;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.quota.size.QuotaAwareDocument.DOCUMENTS_SIZE_STATISTICS_FACET;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.quota.QuotaStatsService;
import org.nuxeo.ecm.quota.size.QuotaAware;
import org.nuxeo.ecm.quota.size.QuotaSizeService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * NXP-11558 : Test that blob can be exclude by their path
 *
 * @author dmetzler
 */
@RunWith(FeaturesRunner.class)
@Features(QuotaFeature.class)
@Deploy("org.nuxeo.ecm.quota.core:exclude-blob-contrib.xml")
public class TestExcludedBlob {

    @Inject
    protected QuotaStatsService qs;

    @Inject
    QuotaSizeService sus;

    @Inject
    CoreSession session;

    @Inject
    EventService eventService;

    protected DocumentRef fileRef;

    protected void next() {
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        eventService.waitForAsyncCompletion(TimeUnit.MINUTES.toMillis(1));
    }

    @Test
    public void quotaServiceCanGiveTheListOfXpathPropsExcludedFromQuotaComputation() throws Exception {
        Collection<String> paths = sus.getExcludedPathList();
        assertThat(paths, is(notNullValue()));
        assertThat(paths.size(), is(1));
        assertThat(paths.contains("files/*/file"), is(true));
    }

    @Test
    public void quotaComputationDontTakeFilesSchemaIntoAccount() throws Exception {

        // Given a document with a blob in the file schema
        DocumentModel doc = session.createDocumentModel("/", "file1", "File");
        doc.setPropertyValue("file:content", (Serializable) getFakeBlob(100));
        doc = session.createDocument(doc);
        fileRef = doc.getRef();
        next();

        doc = getDocument();
        assertQuota(doc, 100, 100);

        // When I add some Blob in the files content
        doc = getDocument();
        List<Map<String, Serializable>> files = new ArrayList<Map<String, Serializable>>();
        for (int i = 1; i < 5; i++) {
            Map<String, Serializable> files_entry = new HashMap<String, Serializable>();
            files_entry.put("file", (Serializable) getFakeBlob(70));
            files.add(files_entry);
        }
        doc.setPropertyValue("files:files", (Serializable) files);
        doc = session.saveDocument(doc);
        next();

        // Then quota should not change
        doc = getDocument();
        assertQuota(doc, 100, 100);
    }

    /**
     * @return
     */
    protected DocumentModel getDocument() {
        return session.getDocument(fileRef);
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

    protected void assertQuota(DocumentModel doc, long innerSize, long totalSize) {
        assertTrue(doc.hasFacet(DOCUMENTS_SIZE_STATISTICS_FACET));
        QuotaAware qa = doc.getAdapter(QuotaAware.class);
        assertNotNull(qa);
        assertEquals("inner:" + innerSize + " total:" + totalSize,
                "inner:" + qa.getInnerSize() + " total:" + qa.getTotalSize());
    }

}
