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

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

/**
 * Interface to be implemented by {@code QuotaStatsUpdater}s registered to the
 * {@link org.nuxeo.ecm.quota.QuotaStatsService}.
 * <p>
 * They use an unrestricted {@link CoreSession} to do the update.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public interface QuotaStatsUpdater {

    /**
     * Update the statistics for the given {@code docCtx} and {@code event}. Signature was changed in 5.6 to pass the
     * Event instead of the eventName to allow the implementer to rollback the transaction if needed
     *
     * @param session an unrestricted {@link CoreSession} to be used
     */
    void updateStatistics(CoreSession session, DocumentEventContext docCtx, Event event);

    /**
     * Compute the initial statistics on the whole repository for this {@code QuotaStatsUpdater}.
     *
     * @param session an unrestricted {@link CoreSession} to be used
     * @deprecated since 10.1, use other signature
     */
    @Deprecated
    default void computeInitialStatistics(CoreSession session, final QuotaStatsInitialWork currentWorker) {
        computeInitialStatistics(session, currentWorker, null);
    }

    /**
     * Compute the initial statistics under the given path for this {@code QuotaStatsUpdater}.
     *
     * @param session an unrestricted {@link CoreSession} to be used
     * @param path the root of the recomputation, or {@code null} for the whole repository
     * @since 10.1
     */
    void computeInitialStatistics(CoreSession session, final QuotaStatsInitialWork currentWorker, String path);

    void setName(String name);

    String getName();

    void setLabel(String label);

    String getLabel();

    void setDescriptionLabel(String descriptionLabel);

    String getDescriptionLabel();

}
