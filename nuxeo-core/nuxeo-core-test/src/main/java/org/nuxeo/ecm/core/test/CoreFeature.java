/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.test;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.CoreSessionService;
import org.nuxeo.ecm.core.api.CoreSessionService.CoreSessionRegistrationInfo;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;
import org.nuxeo.ecm.core.work.WorkManagerFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.model.URLStreamRef;
import org.nuxeo.runtime.stream.RuntimeStreamFeature;
import org.nuxeo.runtime.test.runner.Defaults;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Binder;

/**
 * The core feature provides a default {@link CoreSession} that can be injected.
 * <p>
 * In addition, by injecting the feature itself, some helper methods are available to open new sessions.
 */
@Deploy("org.nuxeo.runtime.management")
@Deploy("org.nuxeo.runtime.metrics")
@Deploy("org.nuxeo.runtime.reload")
@Deploy("org.nuxeo.runtime.kv")
@Deploy("org.nuxeo.runtime.pubsub")
@Deploy("org.nuxeo.runtime.mongodb")
@Deploy("org.nuxeo.runtime.migration")
@Deploy("org.nuxeo.runtime.stream")
@Deploy("org.nuxeo.ecm.core.schema")
@Deploy("org.nuxeo.ecm.core.query")
@Deploy("org.nuxeo.ecm.core.api")
@Deploy("org.nuxeo.ecm.core.event")
@Deploy("org.nuxeo.ecm.core")
@Deploy("org.nuxeo.ecm.core.io")
@Deploy("org.nuxeo.ecm.core.cache")
@Deploy("org.nuxeo.ecm.core.test")
@Deploy("org.nuxeo.ecm.core.mimetype")
@Deploy("org.nuxeo.ecm.core.convert")
@Deploy("org.nuxeo.ecm.core.convert.plugins")
@Deploy("org.nuxeo.ecm.core.storage")
@Deploy("org.nuxeo.ecm.core.storage.sql")
@Deploy("org.nuxeo.ecm.core.storage.sql.test")
@Deploy("org.nuxeo.ecm.core.storage.dbs")
@Deploy("org.nuxeo.ecm.core.storage.mem")
@Deploy("org.nuxeo.ecm.core.storage.mongodb")
@Deploy("org.nuxeo.ecm.platform.commandline.executor")
@Deploy("org.nuxeo.ecm.platform.el")
@RepositoryConfig(cleanup = Granularity.METHOD)
@Features({ RuntimeFeature.class,
        TransactionalFeature.class,
        RuntimeStreamFeature.class,
        WorkManagerFeature.class,
        CoreBulkFeature.class })
public class CoreFeature implements RunnerFeature {

    protected ACP rootAcp;

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
        runner.getFeature(RuntimeFeature.class).registerHandler(new CoreDeployer());

        storageConfiguration = new StorageConfiguration(this);
        txFeature = runner.getFeature(TransactionalFeature.class);
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
            for (String bundle : storageConfiguration.getExternalBundles()) {
                try {
                    harness.deployBundle(bundle);
                } catch (Exception e) {
                    throw new NuxeoException(e);
                }
            }
            URL blobContribUrl = storageConfiguration.getBlobManagerContrib(runner);
            harness.getContext().deploy(new URLStreamRef(blobContribUrl));
            URL repoContribUrl = storageConfiguration.getRepositoryContrib(runner);
            harness.getContext().deploy(new URLStreamRef(repoContribUrl));
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public void beforeRun(FeaturesRunner runner) {
        // wait for async tasks that may have been triggered by
        // RuntimeFeature (typically repo initialization)
        txFeature.nextTransaction(Duration.ofSeconds(10));
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
        binder.bind(CoreSession.class).toProvider(() -> session);
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

        List<CoreSessionRegistrationInfo> leakedInfos = Framework.getService(CoreSessionService.class)
                                                                 .getCoreSessionRegistrationInfos();
        if (leakedInfos.size() == 0) {
            return;
        }
        AssertionError leakedErrors = new AssertionError(String.format("leaked %d sessions", leakedInfos.size()));
        for (CoreSessionRegistrationInfo info : leakedInfos) {
            try {
                ((CloseableCoreSession) info.getCoreSession()).close();
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

    public void waitForAsyncCompletion() {
        txFeature.nextTransaction();
    }

    protected void cleanupSession(FeaturesRunner runner) {
        waitForAsyncCompletion();
        if (session == null) {
            createCoreSession();
        }
        TransactionHelper.runInNewTransaction(() -> {
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
                log.trace(
                        "remove orphan versions as OrphanVersionRemoverListener is not triggered by CoreSession#removeChildren");
                // remove remaining placeless documents
                try (IterableQueryResult results = session.queryAndFetch("SELECT ecm:uuid FROM Document, Relation",
                        NXQL.NXQL)) {
                    batchRemoveDocuments(results);
                }
                // set original ACP on root
                DocumentModel root = session.getRootDocument();
                root.setACP(rootAcp, true);

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
        });
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
        List<DocumentRef> deferredIds = new ArrayList<>();
        for (DocumentRef id : ids) {
            if (!session.exists(id)) {
                continue;
            }
            if (session.canRemoveDocument(id)) {
                session.removeDocument(id);
            } else {
                deferredIds.add(id);
            }
        }
        session.removeDocuments(deferredIds.toArray(new DocumentRef[0]));
    }

    protected void initializeSession(FeaturesRunner runner) {
        if (cleaned) {
            // reinitialize repositories content
            RepositoryService repositoryService = Framework.getService(RepositoryService.class);
            repositoryService.initRepositories();
            cleaned = false;
        }
        CoreScope.INSTANCE.enter();
        createCoreSession();
        if (repositoryInit != null) {
            repositoryInit.populate(session);
            session.save();
            waitForAsyncCompletion();
        }
        // save current root acp
        DocumentModel root = session.getRootDocument();
        rootAcp = root.getACP();
    }

    public String getRepositoryName() {
        return getStorageConfiguration().getRepositoryName();
    }

    public CloseableCoreSession openCoreSession(String username) {
        return CoreInstance.openCoreSession(getRepositoryName(), username);
    }

    public CloseableCoreSession openCoreSession(NuxeoPrincipal principal) {
        return CoreInstance.openCoreSession(getRepositoryName(), principal);
    }

    public CloseableCoreSession openCoreSession() {
        return CoreInstance.openCoreSession(getRepositoryName());
    }

    public CloseableCoreSession openCoreSessionSystem() {
        return CoreInstance.openCoreSessionSystem(getRepositoryName());
    }

    public CloseableCoreSession createCoreSession() {
        UserPrincipal principal = new UserPrincipal("Administrator", new ArrayList<>(), false, true);
        session = CoreInstance.openCoreSession(getRepositoryName(), principal);
        return (CloseableCoreSession) session;
    }

    public CoreSession getCoreSession() {
        return session;
    }

    public void releaseCoreSession() {
        ((CloseableCoreSession) session).close();
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

    public class CoreDeployer extends HotDeployer.ActionHandler {

        @Override
        public void exec(String action, String... agrs) throws Exception {
            waitForAsyncCompletion();
            releaseCoreSession();
            next.exec(action, agrs);
            createCoreSession();
        }

    }

}
