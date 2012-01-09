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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Task doing an initial statistics computation for a defined
 * {@link QuotaStatsUpdater}.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class InitialStatisticsComputationTask implements Runnable {

    private static final Log log = LogFactory.getLog(InitialStatisticsComputationTask.class);

    private final String updaterName;

    private final String repositoryName;

    public InitialStatisticsComputationTask(String updaterName,
            String repositoryName) {
        this.updaterName = updaterName;
        this.repositoryName = repositoryName;
    }

    @Override
    public void run() {
        final QuotaStatsService quotaStatsService = Framework.getLocalService(QuotaStatsService.class);
        TransactionHelper.startTransaction();
        try {
            new UnrestrictedSessionRunner(repositoryName) {
                @Override
                public void run() throws ClientException {
                    quotaStatsService.computeInitialStatistics(updaterName,
                            session);
                }
            }.runUnrestricted();
        } catch (ClientException e) {
            TransactionHelper.setTransactionRollbackOnly();
            log.error(e, e);
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            quotaStatsService.clearProgressStatus(updaterName);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof InitialStatisticsComputationTask) {
            InitialStatisticsComputationTask otherTask = (InitialStatisticsComputationTask) o;
            return updaterName.equals(otherTask.updaterName);
        }
        return false;
    }

}
