/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Default implementation of {@link org.nuxeo.ecm.quota.QuotaStatsService}.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class QuotaStatsServiceImpl extends DefaultComponent implements
        QuotaStatsService {

    private static final Log log = LogFactory.getLog(QuotaStatsServiceImpl.class);

    public static final String QUOTA_STATS_UPDATERS_EP = "quotaStatsUpdaters";

    private QuotaStatsUpdaterRegistry quotaStatsUpdaterRegistry;

    @Override
    public void activate(ComponentContext context) throws Exception {
        quotaStatsUpdaterRegistry = new QuotaStatsUpdaterRegistry();
    }

    @Override
    public void updateStatistics(final DocumentEventContext docCtx,
            final String eventName) {
        List<QuotaStatsUpdater> quotaStatsUpdaters = quotaStatsUpdaterRegistry.getQuotaStatsUpdaters();

        Thread runner = new Thread(new UpdateStatisticsTask(quotaStatsUpdaters,
                docCtx, eventName));
        runner.setDaemon(true);
        runner.start();
        try {
            runner.join(2000);
        } catch (InterruptedException e) {
            log.error("Exit before the end of processing", e);
        }
    }

    @Override
    public void computeInitialStatistics(String repositoryName) {
        try {
            new UnrestrictedSessionRunner(repositoryName) {
                @Override
                public void run() throws ClientException {
                    for (QuotaStatsUpdater updater : quotaStatsUpdaterRegistry.getQuotaStatsUpdaters()) {
                        updater.computeInitialStatistics(session);
                    }
                }
            }.runUnrestricted();
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
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

    private static class UpdateStatisticsTask implements Runnable {

        private static final Log log = LogFactory.getLog(UpdateStatisticsTask.class);

        private final List<QuotaStatsUpdater> quotaStatsUpdaters;

        private final DocumentEventContext docCtx;

        private final String eventName;

        private UpdateStatisticsTask(
                List<QuotaStatsUpdater> quotaStatsUpdaters,
                DocumentEventContext docCtx, String eventName) {
            this.quotaStatsUpdaters = quotaStatsUpdaters;
            this.docCtx = docCtx;
            this.eventName = eventName;
        }

        @Override
        public void run() {
            for (QuotaStatsUpdater updater : quotaStatsUpdaters) {
                Thread runner = new Thread(new UpdaterTask(updater, docCtx,
                        eventName));
                runner.setDaemon(true);
                runner.start();
                try {
                    runner.join();
                } catch (InterruptedException e) {
                    log.error("Exit before the end of processing", e);
                }
            }
        }
    }

}
