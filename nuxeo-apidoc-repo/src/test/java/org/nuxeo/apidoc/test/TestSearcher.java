/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.test;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.repository.RepositoryDistributionSnapshot;
import org.nuxeo.apidoc.search.ArtifactSearcher;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(RuntimeSnaphotFeature.class)
public class TestSearcher {

    @Inject
    protected CoreSession session;

    @Inject
    protected ArtifactSearcher searcher;

    @Inject
    protected SnapshotManager snapshotManager;

    @Inject
    protected EventService eventService;

    @Inject
    protected WorkManager workManager;

    @Inject
    protected ElasticSearchAdmin esa;

    /** Wait for async worker completion and ES indexing completion. */
    protected void waitForIndexing() throws Exception {
        TransactionHelper.commitOrRollbackTransaction();
        workManager.awaitCompletion(20, TimeUnit.SECONDS);
        esa.prepareWaitForIndexing().get(20, TimeUnit.SECONDS);
        esa.refresh();
        TransactionHelper.startTransaction();
    }

    @Test
    public void testSearch() throws Exception {
        DistributionSnapshot persistent = snapshotManager.persistRuntimeSnapshot(session);
        assertNotNull(persistent);
        waitForIndexing();

        List<String> componentIds = persistent.getComponentIds();
        assertNotEquals(0, componentIds.size());

        // Is fulltext ready
        DocumentModel dist = ((RepositoryDistributionSnapshot) persistent).getDoc();
        String query = String.format("select * from Document where ecm:fulltext = 'a*' and ecm:path STARTSWITH '%s'",
                dist.getPathAsString());
        assertNotEquals(0, session.query(query));

        List<NuxeoArtifact> artifacts = searcher.searchArtifact(session, persistent.getKey(), "related");
        assertNotEquals(0, artifacts.size());

        artifacts = searcher.filterArtifact(session, persistent.getKey(), "NXComponent", "related");
        assertNotEquals(0, artifacts.size());
    }

}
