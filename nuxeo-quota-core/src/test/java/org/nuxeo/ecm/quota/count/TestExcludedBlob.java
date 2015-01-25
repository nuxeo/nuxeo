/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.annotations.TransactionalConfig;
import org.nuxeo.ecm.quota.QuotaStatsService;
import org.nuxeo.ecm.quota.size.QuotaAware;
import org.nuxeo.ecm.quota.size.QuotaSizeService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * NXP-11558 : Test that blob can be exclude by their path
 *
 * @author dmetzler
 */
@RunWith(FeaturesRunner.class)
@Features(QuotaFeature.class)
@LocalDeploy("org.nuxeo.ecm.quota.core:exclude-blob-contrib.xml")
@TransactionalConfig(autoStart = false)
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

    private IsolatedSessionRunner isr;

    @Before
    public void cleanupSessionAssociationBeforeTest() throws Exception {
        isr = new IsolatedSessionRunner(session, eventService);
        assertThat(sus, is(notNullValue()));
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
        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {
                DocumentModel doc = session.createDocumentModel("/", "file1", "File");
                doc.setPropertyValue("file:content", (Serializable) getFakeBlob(100));
                doc = session.createDocument(doc);
                fileRef = doc.getRef();

            }
        });

        isr.run(new RunnableWithException() {
            @Override
            public void run() throws Exception {
                DocumentModel doc = getDocument();
                assertQuota(doc, 100, 100);
            }
        });

        // When I add some Blob in the files content
        isr.run(new RunnableWithException() {
            @Override
            public void run() throws Exception {
                DocumentModel doc = getDocument();
                List<Map<String, Serializable>> files = new ArrayList<Map<String, Serializable>>();

                for (int i = 1; i < 5; i++) {
                    Map<String, Serializable> files_entry = new HashMap<String, Serializable>();
                    files_entry.put("filename", "fakefile" + i);
                    files_entry.put("file", (Serializable) getFakeBlob(70));
                    files.add(files_entry);
                }

                doc.setPropertyValue("files:files", (Serializable) files);
                doc = session.saveDocument(doc);

            }
        });

        // Then quota should not change
        isr.run(new RunnableWithException() {
            @Override
            public void run() throws Exception {
                DocumentModel doc = getDocument();
                assertQuota(doc, 100, 100);
            }
        });

    }

    /**
     * @return
     * @throws ClientException
     */
    protected DocumentModel getDocument() throws ClientException {
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
