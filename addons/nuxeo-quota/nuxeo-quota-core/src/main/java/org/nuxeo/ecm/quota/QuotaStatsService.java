/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.quota;

import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
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
    void updateStatistics(DocumentEventContext docCtx, Event event);

    /**
     * Compute the initial statistics for the given @{code updaterName}.
     *
     * @deprecated since 10.1, use other signature
     */
    @Deprecated
    default void computeInitialStatistics(String updaterName, CoreSession session, QuotaStatsInitialWork currentWorker) {
        computeInitialStatistics(updaterName, session, currentWorker, null);
    }

    /**
     * Compute the initial statistics for the given @{code updaterName} for {@code docPath}.
     *
     * @since 10.1
     */
    void computeInitialStatistics(String updaterName, CoreSession session, QuotaStatsInitialWork currentWorker, String path);

    /**
     * Launch an asynchronously initial computation for the given {@code updaterName} on {@code repositoryName}.
     *
     * @deprecated since 10.1, use other signature
     */
    @Deprecated
    default void launchInitialStatisticsComputation(String updaterName, String repositoryName) {
        launchInitialStatisticsComputation(updaterName, repositoryName, null);
    }

    /**
     * Launch an asynchronously initial computation for the given {@code updaterName} on {@code repositoryName}
     * for {@code docPath}.
     *
     * @since 10.1
     */
    void launchInitialStatisticsComputation(String updaterName, String repositoryName, String path);

    /**
     * Returns the progress status of {@code updaterName}.
     */
    String getProgressStatus(String updaterName, String repositoryName);

    /**
     * Gets the quota from the first parent where quota has been set. Returns -1 if no quota has been set. For user
     * workspaces, only the first parent is investigated
     *
     * @since 5.7
     */
    long getQuotaFromParent(DocumentModel doc, CoreSession session);

    /**
     * Test to see if quota allowed. Skip user worskpaces, where validation rules don't apply.
     *
     * @since 5.7
     */
    boolean canSetMaxQuota(long maxQuota, DocumentModel doc, CoreSession session);

    /**
     * Sets this maxQuota on all user workspaces
     *
     * @since 5.7
     */
    void launchSetMaxQuotaOnUserWorkspaces(long maxQuota, DocumentModel context, CoreSession session);

    /**
     * Activates the quota on user personal workspaces
     *
     * @since 5.7
     */
    void activateQuotaOnUserWorkspaces(long maxQuota, CoreSession session);

    /**
     * @since 5.7
     */
    long getQuotaSetOnUserWorkspaces(CoreSession session);

}
