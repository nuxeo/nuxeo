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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.quota;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.runtime.api.Framework;

/**
 * Work doing an initial statistics computation for a defined
 * {@link QuotaStatsUpdater}.
 * 
 * @since 5.6
 */
public class QuotaStatsInitialWork extends AbstractWork {

    public static final String CATEGORY_QUOTA_INITIAL = "quotaInitialStatistics";

    private final String updaterName;

    private final String repositoryName;

    public QuotaStatsInitialWork(String updaterName, String repositoryName) {
        this.updaterName = updaterName;
        this.repositoryName = repositoryName;
    }

    @Override
    public String getCategory() {
        return CATEGORY_QUOTA_INITIAL;
    }

    @Override
    public String getTitle() {
        return "Quota Statistics " + updaterName;
    }

    public void notifyProgress(float percent) {
        setProgress(new Progress(percent));
    }

    public void notifyProgress(long current, long total) {
        setProgress(new Progress(current, total));
    }

    @Override
    public void work() throws ClientException {
        final QuotaStatsInitialWork currentWorker = this;
        new UnrestrictedSessionRunner(repositoryName) {
            @Override
            public void run() throws ClientException {
                QuotaStatsService service = Framework.getLocalService(QuotaStatsService.class);
                service.computeInitialStatistics(updaterName, session,
                        currentWorker);
            }
        }.runUnrestricted();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof QuotaStatsInitialWork)) {
            return false;
        }
        QuotaStatsInitialWork other = (QuotaStatsInitialWork) object;
        return new EqualsBuilder().append(updaterName, other.updaterName).append(
                repositoryName, other.repositoryName).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(updaterName).append(repositoryName).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("updaterName", updaterName).append(
                "repositoryName", repositoryName).toString();
    }

}
