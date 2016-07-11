/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.test;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.CoreSessionService;
import org.nuxeo.ecm.core.api.CoreSessionService.CoreSessionRegistrationInfo;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.test.TransactionalFeature.Waiter;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.model.URLStreamRef;
import org.nuxeo.runtime.test.runner.Defaults;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.test.runner.SimpleFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Binder;
import com.google.inject.Provider;

/**
 * The core feature provides a default {@link CoreSession} that can be injected.
 * <p>
 * In addition, by injecting the feature itself, some helper methods are available to open new sessions.
 */
@Deploy({ "org.nuxeo.runtime.management", //
        "org.nuxeo.runtime.metrics",
        "org.nuxeo.ecm.core.schema", //
        "org.nuxeo.ecm.core.query", //
        "org.nuxeo.ecm.core.api", //
        "org.nuxeo.ecm.core.event", //
        "org.nuxeo.ecm.core", //
        "org.nuxeo.ecm.core.test", //
        "org.nuxeo.ecm.core.mimetype", //
        "org.nuxeo.ecm.core.convert", //
        "org.nuxeo.ecm.core.convert.plugins", //
        "org.nuxeo.ecm.core.storage", //
        "org.nuxeo.ecm.core.storage.sql", //
        "org.nuxeo.ecm.core.storage.sql.test", //
        "org.nuxeo.ecm.core.storage.dbs", //
        "org.nuxeo.ecm.core.storage.mem", //
        "org.nuxeo.ecm.core.storage.mongodb", //
})
@Features({ RuntimeFeature.class, TransactionalFeature.class })
@LocalDeploy("org.nuxeo.ecm.core.event:test-queuing.xml")
public class CoreFeature extends SimpleFeature {

    private static final Log log = LogFactory.getLog(CoreFeature.class);

    protected StorageConfiguration storageConfiguration;

    protected RepositoryInit repositoryInit;

    protected Granularity granularity;

    // this value gets injected
    protected CoreSession session;

    protected boolean cleaned;

    protected TransactionalFeature txFeature;

    public StorageConfiguration getStorageConfiguration() {
        return storageConfiguration;
    }

    @Override
    public void initialize(FeaturesRunner runner) {
        storageConfiguration = new StorageConfiguration(this);
        txFeature = runner.getFeature(TransactionalFeature.class);
        txFeature.addWaiter(new Waiter() {

            @Override
            public boolean await(long deadline) throws InterruptedException {
                return Framework.getService(WorkManager.class)
                        .awaitCompletion(deadline - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            }

        });
        // init from RepositoryConfig annotations
        RepositoryConfig repositoryConfig = runner.getConfig(RepositoryConfig.class);
        if (repositoryConfig == null) {
            repositoryConfig = Defaults.of(RepositoryConfig.class);
        }
        try {
            repositoryInit = repositoryConfig.init().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException(e);
        }
        Granularity cleanup = repositoryConfig.cleanup();
        granularity = cleanup == Granularity.UNDEFINED ? Granularity.CLASS : cleanup;
    }


    public Granularity getGranularity() {
        return granularity;
    }

    @Override
    public void start(FeaturesRunner runner) {
        try {
            RuntimeHarness harness = runner.getFeature(RuntimeFeature.class).getHarness();
            storageConfiguration.init();
            URL blobContribUrl = storageConfiguration.getBlobManagerContrib(runner);
            harness.getContext().deploy(new URLStreamRef(blobContribUrl));
            URL repoContribUrl = storageConfiguration.getRepositoryContrib(runner);
            harness.getContext().deploy(new URLStreamRef(repoContribUrl));
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public void beforeRun(FeaturesRunner runner) throws InterruptedException {
        // wait for async tasks that may have been triggered by
        // RuntimeFeature (typically repo initialization)
        txFeature.nextTransaction(10, TimeUnit.SECONDS);
        if (granularity != Granularity.METHOD) {
            // we need a transaction to properly initialize the session
            // but it hasn't been started yet by TransactionalFeature
            TransactionHelper.startTransaction();
            initializeSession(runner);
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        binder.bind(CoreSession.class).toProvider(new Provider<CoreSession>() {
            @Override
            public CoreSession get() {
                return session;
            }
        });
    }

    @Override
    public void afterRun(FeaturesRunner runner) {
        waitForAsyncCompletion(); // fulltext and various workers
        if (granularity != Granularity.METHOD) {
            cleanupSession(runner);
        }
        if (session != null) {
            releaseCoreSession();
        }

        List<CoreSessionRegistrationInfo> leakedInfos = Framework.getService(
                CoreSessionService.class).getCoreSessionRegistrationInfos();
        if (leakedInfos.size() == 0) {
            return;
        }
        AssertionError leakedErrors = new AssertionError(String.format("leaked %d sessions", leakedInfos.size()));
        for (CoreSessionRegistrationInfo info:leakedInfos) {
            try {
                info.getCoreSession().close();
                leakedErrors.addSuppressed(info);
            } catch (RuntimeException cause) {
                leakedErrors.addSuppressed(cause);
            }
        }
        throw leakedErrors;
    }

    @Override
    public void beforeSetup(FeaturesRunner runner) {
        if (granularity == Granularity.METHOD) {
            initializeSession(runner);
        }
    }

    @Override
    public void afterTeardown(FeaturesRunner runner) {
        if (granularity == Granularity.METHOD) {
            cleanupSession(runner);
        } else {
            waitForAsyncCompletion();
        }
    }

    protected void waitForAsyncCompletion() {
        txFeature.nextTransaction();
    }

    protected void cleanupSession(FeaturesRunner runner) {
        waitForAsyncCompletion();
        if (TransactionHelper.isTransactionMarkedRollback()) { // ensure tx is
                                                               // active
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        if (session == null) {
            createCoreSession();
        }
        try {
            log.trace("remove everything except root");
            // remove proxies first, as we cannot remove a target if there's a proxy pointing to it
            try (IterableQueryResult results = session.queryAndFetch(
                    "SELECT ecm:uuid FROM Document WHERE ecm:isProxy = 1", NXQL.NXQL)) {
                batchRemoveDocuments(results);
            } catch (QueryParseException e) {
                // ignore, proxies disabled
            }
            // remove non-proxies
            session.removeChildren(new PathRef("/"));
            log.trace("remove orphan versions as OrphanVersionRemoverListener is not triggered by CoreSession#removeChildren");
            // remove remaining placeless documents
            try (IterableQueryResult results = session.queryAndFetch("SELECT ecm:uuid FROM Document, Relation",
                    NXQL.NXQL)) {
                batchRemoveDocuments(results);
            }
            session.save();
            waitForAsyncCompletion();
            if (!session.query("SELECT * FROM Document, Relation").isEmpty()) {
                log.error("Fail to cleanupSession, repository will not be empty for the next test.");
            }
        } catch (NuxeoException e) {
            log.error("Unable to reset repository", e);
        } finally {
            CoreScope.INSTANCE.exit();
        }
        releaseCoreSession();
        cleaned = true;
    }

    protected void batchRemoveDocuments(IterableQueryResult results) {
        String rootDocumentId = session.getRootDocument().getId();
        List<DocumentRef> ids = new ArrayList<>();
        for (Map<String, Serializable> result : results) {
            String id = (String) result.get("ecm:uuid");
            if (id.equals(rootDocumentId)) {
                continue;
            }
            ids.add(new IdRef(id));
            if (ids.size() >= 100) {
                batchRemoveDocuments(ids);
                ids.clear();
            }
        }
        if (!ids.isEmpty()) {
            batchRemoveDocuments(ids);
        }
    }

    protected void batchRemoveDocuments(List<DocumentRef> ids) {
        session.removeDocuments(ids.toArray(new DocumentRef[0]));
    }

    protected void initializeSession(FeaturesRunner runner) {
        if (cleaned) {
            // re-trigger application started
            RepositoryService repositoryService = Framework.getLocalService(RepositoryService.class);
            repositoryService.applicationStarted(null);
            cleaned = false;
        }
        CoreScope.INSTANCE.enter();
        createCoreSession();
        if (repositoryInit != null) {
            repositoryInit.populate(session);
            session.save();
            waitForAsyncCompletion();
        }
    }

    public String getRepositoryName() {
        return getStorageConfiguration().getRepositoryName();
    }

    public CoreSession openCoreSession(String username) {
        return CoreInstance.openCoreSession(getRepositoryName(), username);
    }

    public CoreSession openCoreSession(NuxeoPrincipal principal) {
        return CoreInstance.openCoreSession(getRepositoryName(), principal);
    }

    public CoreSession openCoreSession() {
        return CoreInstance.openCoreSession(getRepositoryName());
    }

    public CoreSession openCoreSessionSystem() {
        return CoreInstance.openCoreSessionSystem(getRepositoryName());
    }

    public CoreSession createCoreSession() {
        UserPrincipal principal = new UserPrincipal("Administrator", new ArrayList<String>(), false, true);
        session = CoreInstance.openCoreSession(getRepositoryName(), principal);
        return session;
    }

    public CoreSession getCoreSession() {
        return session;
    }

    public void releaseCoreSession() {
        session.close();
        session = null;
    }

    public CoreSession reopenCoreSession() {
        releaseCoreSession();
        waitForAsyncCompletion();
        // flush JCA cache to acquire a new low-level session
        NuxeoContainer.resetConnectionManager();
        createCoreSession();
        return session;
    }

}
