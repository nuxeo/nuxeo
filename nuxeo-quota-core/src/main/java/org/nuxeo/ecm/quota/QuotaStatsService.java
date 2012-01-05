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

import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

/**
 * Service used to compute quota and statistics on documents.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public interface QuotaStatsService {

    /**
     * Update the statistics for the given {@code docCtx} and {@code eventName}.
     * <p>
     * Call all the registered {@link org.nuxeo.ecm.quota.QuotaStatsUpdater}s.
     */
    void updateStatistics(DocumentEventContext docCtx, String eventName);

    /**
     * Compute the initial statistics for the given @{code repositoryName}.
     * <p>
     * Call all the registered {@link org.nuxeo.ecm.quota.QuotaStatsUpdater}s.
     */
    void computeInitialStatistics(String repositoryName);

}
