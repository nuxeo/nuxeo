/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.elasticsearch.test.versioning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * @since 2025.14
 */
@RunWith(FeaturesRunner.class)
@Features(RepositoryElasticSearchFeature.class)
@WithFrameworkProperty(name = "elasticsearch.reindexVersionsListener.enabled", value = "true")
public class TestReindexVersionOnMove {

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected ElasticSearchAdmin esa;

    protected void assertVersionWithPath(String path) {
        NxQueryBuilder queryBuilder = new NxQueryBuilder(session).nxql(
                "SELECT * FROM Document WHERE ecm:path = '%s' AND ecm:isVersion = 1".formatted(path))
                                                                 .fetchFromElasticsearch();
        DocumentModelList results = ess.query(queryBuilder);
        assertThat(results).hasSize(1);
    }

    @Before
    public void setupIndex() {
        esa.initIndexes(true);
    }

    @Test
    public void shouldIndexVersion() throws Exception {
        // Create doc
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);
        // Create version
        DocumentRef version = doc.checkIn(VersioningOption.MAJOR, null);
        DocumentModel versionDoc = session.getDocument(version);
        // Live document and its version have the same path
        assertEquals(doc.getPath(), versionDoc.getPath());

        txFeature.nextTransaction();

        // Create a folder
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        // Move the live doc into new folder
        session.move(doc.getRef(), folder.getRef(), "doc1");
        session.save();

        txFeature.nextTransaction();

        assertVersionWithPath("/folder/doc1");
    }

    @Test
    public void shouldIndexNestedVersion() throws Exception {
        // Create a folder 1
        DocumentModel folder1 = session.createDocumentModel("/", "folder1", "Folder");
        folder1 = session.createDocument(folder1);
        // Create doc
        DocumentModel doc = session.createDocumentModel("/folder1", "doc", "File");
        doc = session.createDocument(doc);
        // Create version
        DocumentRef version = doc.checkIn(VersioningOption.MAJOR, null);
        DocumentModel versionDoc = session.getDocument(version);
        // Live document and its version have the same path
        assertEquals(doc.getPath(), versionDoc.getPath());

        txFeature.nextTransaction();

        // Create a folder 2
        DocumentModel folder2 = session.createDocumentModel("/", "folder2", "Folder");
        folder2 = session.createDocument(folder2);
        // Move the folder1 into new folder2
        session.move(folder1.getRef(), folder2.getRef(), "folder11");
        session.save();

        txFeature.nextTransaction();

        assertVersionWithPath("/folder2/folder11/doc");
    }
}
