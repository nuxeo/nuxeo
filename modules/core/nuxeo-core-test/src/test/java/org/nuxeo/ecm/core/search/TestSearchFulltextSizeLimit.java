/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 */

package org.nuxeo.ecm.core.search;

import static org.junit.Assume.assumeTrue;
import static org.nuxeo.ecm.core.search.BaseCoreSearchFeature.newSearchQuery;
import static org.nuxeo.ecm.core.search.index.DefaultIndexingJsonWriter.MAX_FULLTEXT_SIZE_FIELD;

import jakarta.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.CoreSearchFeature;
import org.nuxeo.runtime.test.runner.ConditionalIgnore;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * Test that fulltext can be truncated.
 *
 * @since 2025.8
 */
@RunWith(FeaturesRunner.class)
@Features(CoreSearchFeature.class)
@WithFrameworkProperty(name = MAX_FULLTEXT_SIZE_FIELD, value = "21")
@ConditionalIgnore(condition = IgnoreIfSearchClientDoesNotHaveIndexingCapability.class)
public class TestSearchFulltextSizeLimit {

    @Inject
    protected CoreSession session;

    @Inject
    protected SearchService searchService;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSearchFeature coreSearchFeature;

    @Inject
    protected TransactionalFeature txFeature;

    @Before
    public void checkSupportsFulltextSearchIfRepositoryClient() {
        assumeTrue("fulltext search not supported", !coreSearchFeature.hasRepositoryClient()
                || coreFeature.getStorageConfiguration().supportsFulltextSearch());
    }

    @Test
    public void testFulltext() {
        createFileWithBlob();
        String nxql = "SELECT * FROM Document WHERE ecm:fulltext='search'";
        DocumentModelList esRet = searchService.search(newSearchQuery(session, nxql)).loadDocuments(session);
        Assert.assertEquals(1, esRet.totalSize());
        // fulltext is truncated to " You know for search "
        nxql = "SELECT * FROM Document WHERE ecm:fulltext='limited size'";
        esRet = searchService.search(newSearchQuery(session, nxql)).loadDocuments(session);
        Assert.assertEquals(0, esRet.totalSize());
    }

    protected void createFileWithBlob() {
        DocumentModel doc = session.createDocumentModel("/", "myFile", "File");
        BlobHolder holder = doc.getAdapter(BlobHolder.class);
        // only the first part of the blob will be indexed.
        holder.setBlob(new StringBlob("You know for search but limited in size"));
        session.createDocument(doc);
        txFeature.nextTransaction();
    }

}
