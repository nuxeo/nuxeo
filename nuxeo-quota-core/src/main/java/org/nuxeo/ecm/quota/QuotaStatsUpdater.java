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
     * Update the statistics for the given {@code docCtx} and {@code event}.
     * Signature was changed in 5.6 to pass the Event instead of the eventName
     * to allow the implementer to rollback the transaction if needed
     * 
     * @param session an unrestricted {@link CoreSession} to be used
     */
    void updateStatistics(CoreSession session, DocumentEventContext docCtx,
            Event event) throws ClientException;

    /**
     * Compute the initial statistics on the whole repository for this
     * {@code QuotaStatsUpdater}.
     * 
     * @param session an unrestricted {@link CoreSession} to be used
     */
    void computeInitialStatistics(CoreSession session,
            final QuotaStatsInitialWork currentWorker);

    public void setName(String name);

    public String getName();

    public void setLabel(String label);

    public String getLabel();

    public void setDescriptionLabel(String descriptionLabel);

    public String getDescriptionLabel();

}
