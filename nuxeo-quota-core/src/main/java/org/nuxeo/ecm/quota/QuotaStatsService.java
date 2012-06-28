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

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

/**
 * Service used to compute quota and statistics on documents.
 * 
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public interface QuotaStatsService {

    List<QuotaStatsUpdater> getQuotaStatsUpdaters();

    /**
     * Update the statistics for the given {@code docCtx} and {@code event}.
     * <p>
     * Call all the registered {@link org.nuxeo.ecm.quota.QuotaStatsUpdater}s.
     */
    void updateStatistics(DocumentEventContext docCtx, Event event)
            throws ClientException;

    /**
     * Compute the initial statistics for the given @{code updaterName}.
     */
    void computeInitialStatistics(String updaterName, CoreSession session);

    /**
     * Launch an asynchronously initial computation for the given
     * {@code updaterName} on {@code repositoryName}.
     */
    void launchInitialStatisticsComputation(String updaterName,
            String repositoryName);

    /**
     * Returns the progress status of {@code updaterName}.
     */
    String getProgressStatus(String updaterName);

}
