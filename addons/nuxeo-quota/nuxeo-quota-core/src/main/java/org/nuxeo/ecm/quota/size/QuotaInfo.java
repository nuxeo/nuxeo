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
 *     <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */

package org.nuxeo.ecm.quota.size;

/**
 * Helper class to have easy to display numbers and stats
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.6
 * 
 */
public class QuotaInfo {

    protected final QuotaDisplayValue innerSize;

    protected final QuotaDisplayValue totalSize;

    protected final QuotaDisplayValue maxQuota;

    public QuotaInfo(long innerSize, long totalSize, long maxQuota) {
        this.innerSize = new QuotaDisplayValue(innerSize, maxQuota);
        this.totalSize = new QuotaDisplayValue(totalSize, maxQuota);
        this.maxQuota = new QuotaDisplayValue(maxQuota);
    }

    public QuotaDisplayValue getInnerSize() {
        return innerSize;
    }

    public QuotaDisplayValue getTotalSize() {
        return totalSize;
    }

    public QuotaDisplayValue getMaxQuota() {
        return maxQuota;
    }

}
