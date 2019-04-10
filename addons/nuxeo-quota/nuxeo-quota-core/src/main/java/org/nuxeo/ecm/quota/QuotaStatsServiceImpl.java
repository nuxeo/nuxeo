/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.quota;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.event.impl.AsyncEventExecutor;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.common.collect.MapMaker;

/**
 * Default implementation of {@link org.nuxeo.ecm.quota.QuotaStatsService}.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class QuotaStatsServiceImpl extends DefaultComponent implements
        QuotaStatsService {

    private static final Log log = LogFactory.getLog(QuotaStatsServiceImpl.class);

    public static final String STATUS_INITIAL_COMPUTATION_QUEUED = "status.quota.initialComputationQueued";

    public static final String STATUS_INITIAL_COMPUTATION_PENDING = "status.quota.initialComputationInProgress";

    public static final String QUOTA_STATS_UPDATERS_EP = "quotaStatsUpdaters";

    public static final int DEFAULT_TIMEOUT = 2;

    private static Integer timeout;

    private QuotaStatsUpdaterRegistry quotaStatsUpdaterRegistry;

    private BlockingQueue<Runnable> updaterTaskQueue;

    private ThreadPoolExecutor updaterExecutor;

    private final Map<String, String> states = new MapMaker().concurrencyLevel(
            10).expiration(1, TimeUnit.DAYS).makeMap();

    private static int getUpdatersRunnerTimeOutInS() {
        if (timeout == null) {
            String strTimeout = Framework.getProperty(
                    "org.nuxeo.ecm.quota.updaters.runner.timeout",
                    String.valueOf(DEFAULT_TIMEOUT));
            try {
                timeout = Integer.parseInt(strTimeout);
            } catch (NumberFormatException e) {
                timeout = DEFAULT_TIMEOUT;
            }
        }
        return timeout;
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        quotaStatsUpdaterRegistry = new QuotaStatsUpdaterRegistry();

        AsyncEventExecutor.NamedThreadFactory serializationThreadFactory = new AsyncEventExecutor.NamedThreadFactory(
                "Nuxeo Async Statistics Computation");
        updaterTaskQueue = new LinkedBlockingQueue<Runnable>();
        updaterExecutor = new ThreadPoolExecutor(1, 1, 5, TimeUnit.MINUTES,
                updaterTaskQueue, serializationThreadFactory);
    }

    @Override
    public List<QuotaStatsUpdater> getQuotaStatsUpdaters() {
        return quotaStatsUpdaterRegistry.getQuotaStatsUpdaters();
    }

    @Override
    public void updateStatistics(final DocumentEventContext docCtx,
            final String eventName) {
        List<QuotaStatsUpdater> quotaStatsUpdaters = quotaStatsUpdaterRegistry.getQuotaStatsUpdaters();

        Thread runner = new Thread(new UpdateStatisticsRunner(
                quotaStatsUpdaters, docCtx, eventName));
        runner.setDaemon(true);
        runner.start();
        try {
            runner.join(getUpdatersRunnerTimeOutInS() * 1000);
        } catch (InterruptedException e) {
            log.error("Exit before the end of processing", e);
        }
    }

    @Override
    public void computeInitialStatistics(String updaterName, CoreSession session) {
        if (states.containsKey(updaterName)) {
            states.put(updaterName, STATUS_INITIAL_COMPUTATION_PENDING);
        }

        QuotaStatsUpdater updater = quotaStatsUpdaterRegistry.getQuotaStatsUpdater(updaterName);
        if (updater != null) {
            updater.computeInitialStatistics(session);
        }
    }

    @Override
    public void launchInitialStatisticsComputation(String updaterName,
            String repositoryName) {
        InitialStatisticsComputationTask task = new InitialStatisticsComputationTask(
                updaterName, repositoryName);
        if (!updaterTaskQueue.contains(task)) {
            states.put(updaterName, STATUS_INITIAL_COMPUTATION_QUEUED);
            updaterExecutor.execute(task);
        }
    }

    @Override
    public String getProgressStatus(String updaterName) {
        return states.get(updaterName);
    }

    @Override
    public void clearProgressStatus(String updaterName) {
        states.remove(updaterName);
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (QUOTA_STATS_UPDATERS_EP.equals(extensionPoint)) {
            quotaStatsUpdaterRegistry.addContribution((QuotaStatsUpdaterDescriptor) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (QUOTA_STATS_UPDATERS_EP.equals(extensionPoint)) {
            quotaStatsUpdaterRegistry.removeContribution((QuotaStatsUpdaterDescriptor) contribution);
        }
    }

    private static class UpdateStatisticsRunner implements Runnable {

        private static final Log log = LogFactory.getLog(UpdateStatisticsRunner.class);

        private final List<QuotaStatsUpdater> quotaStatsUpdaters;

        private final DocumentEventContext docCtx;

        private final String eventName;

        private UpdateStatisticsRunner(
                List<QuotaStatsUpdater> quotaStatsUpdaters,
                DocumentEventContext docCtx, String eventName) {
            this.quotaStatsUpdaters = quotaStatsUpdaters;
            this.docCtx = docCtx;
            this.eventName = eventName;
        }

        @Override
        public void run() {
            for (final QuotaStatsUpdater updater : quotaStatsUpdaters) {
                TransactionHelper.startTransaction();
                try {
                    new UnrestrictedSessionRunner(docCtx.getRepositoryName()) {
                        @Override
                        public void run() throws ClientException {
                            updater.updateStatistics(session, docCtx, eventName);
                        }
                    }.runUnrestricted();
                } catch (ClientException e) {
                    TransactionHelper.setTransactionRollbackOnly();
                    log.error(e, e);
                } finally {
                    TransactionHelper.commitOrRollbackTransaction();
                }
            }
        }
    }

}
