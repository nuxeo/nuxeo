/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin
 */
package org.nuxeo.ecm.core.management.jtajca;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features({ JtajcaManagementFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
public class IndexerDoesNotLeakTest {

    @Inject
    CoreSession repo;

    @Inject
    WorkManager works;

    @Inject
    @Named("repository/test")
    ConnectionPoolMonitor repoMonitor;

    @Inject
    @Named("jdbc/nuxeojunittests")
    ConnectionPoolMonitor dbMonitor;

    @Test
    public void indexerWorkDoesNotLeak() throws InterruptedException {
        int repoCount = repoMonitor.getConnectionCount();
        int dbCount = dbMonitor.getConnectionCount();
        DocumentModel doc = repo.createDocumentModel("/", "note", "Note");
        repo.createDocument(doc);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        works.awaitCompletion(10, TimeUnit.SECONDS);
        assertThat(repoCount, is(repoMonitor.getConnectionCount()));
        assertThat(dbCount, is(dbMonitor.getConnectionCount()));

    }
}
