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

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Default implementation of {@link QuotaStatsService}.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class QuotaStatsServiceImpl extends DefaultComponent implements
        QuotaStatsService {

    public static final String QUOTA_STATS_UPDATERS_EP = "quotaStatsUpdaters";

    private QuotaStatsUpdaterRegistry quotaStatsUpdaterRegistry;

    @Override
    public void activate(ComponentContext context) throws Exception {
        quotaStatsUpdaterRegistry = new QuotaStatsUpdaterRegistry();
    }

    @Override
    public void updateStatistics(final DocumentEventContext docCtx,
            final String eventName) {
        try {
            new UnrestrictedSessionRunner(docCtx.getRepositoryName()) {
                @Override
                public void run() throws ClientException {
                    for (QuotaStatsUpdater updater : quotaStatsUpdaterRegistry.getQuotaStatsUpdaters()) {
                        updater.updateStatistics(session, docCtx, eventName);
                    }
                }
            }.runUnrestricted();
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
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

}
