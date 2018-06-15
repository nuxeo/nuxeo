/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.elasticsearch.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.elasticsearch.core.test:elasticsearch-test-contrib.xml")

/**
 * Tests the indexing of proxies
 */
public class TestProxiesIndexing {
    @Inject
    protected CoreSession coreSession;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected ElasticSearchAdmin esa;

    @Inject
    protected WorkManager workManager;

    @Test
    public void testES() throws InterruptedException {
        DocumentModel note = init();

        NxQueryBuilder queryBuilder = new NxQueryBuilder(coreSession).nxql("SELECT * FROM Note WHERE ecm:isProxy = 1")
                .fetchFromElasticsearch();
        DocumentModelList results = ess.query(queryBuilder);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPropertyValue("dc:title")).isEqualTo("Titre 1");

        update(note);

        results = ess.query(queryBuilder);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPropertyValue("dc:title")).isEqualTo("Titre 2");
    }

    @Test
    public void testVCS() throws InterruptedException {
        DocumentModel note = init();

        DocumentModelList results = coreSession.query("SELECT * FROM Note WHERE ecm:isProxy = 1");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPropertyValue("dc:title")).isEqualTo("Titre 1");
        DocumentModel proxy = results.get(0);
        update(note);

        proxy = coreSession.getDocument(proxy.getRef());
        assertThat(proxy.getPropertyValue("dc:title")).isEqualTo("Titre 2");

        results = coreSession.query("SELECT * FROM Note WHERE ecm:isProxy = 1");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPropertyValue("dc:title")).isEqualTo("Titre 2");

    }

    protected DocumentModel init() throws InterruptedException {
        DocumentModel folder = coreSession.createDocumentModel("/", "folder", "Folder");
        folder = coreSession.createDocument(folder);

        DocumentModel note = coreSession.createDocumentModel("/", "note", "Note");
        note.setPropertyValue("dc:title", "Titre 1");
        note = coreSession.createDocument(note);

        coreSession.createProxy(note.getRef(), folder.getRef());

        coreSession.save();
        waitForElasticsearch();

        return note;
    }

    protected void update(DocumentModel note) throws InterruptedException {
        note = coreSession.getDocument(note.getRef());
        note.setPropertyValue("dc:title", "Titre 2");
        coreSession.saveDocument(note);

        coreSession.save();
        waitForElasticsearch();
    }

    protected void waitForElasticsearch() throws InterruptedException {
        TransactionHelper.commitOrRollbackTransaction();

        assertThat(workManager.awaitCompletion(10, TimeUnit.SECONDS)).isTrue();
        assertThat(esa.getPendingWorkerCount()).isEqualTo(0);
        esa.refresh();

        TransactionHelper.startTransaction();
    }
}
