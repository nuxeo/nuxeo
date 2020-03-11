/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.quota.automation;

import org.nuxeo.ecm.quota.size.QuotaInfo;

public class SimpleQuotaInfo {

    protected long innerSize;

    protected long totalSize;

    protected long maxQuota;

    public SimpleQuotaInfo() {
        innerSize = -1;
        totalSize = -1;
        maxQuota = -1;
    }

    public SimpleQuotaInfo(QuotaInfo info) {
        innerSize = info.getInnerSize().getValue();
        totalSize = info.getTotalSize().getValue();
        maxQuota = info.getMaxQuota().getValue();
    }

    public long getInnerSize() {
        return innerSize;
    }

    public void setInnerSize(long innerSize) {
        this.innerSize = innerSize;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public long getMaxQuota() {
        return maxQuota;
    }

    public void setMaxQuota(long maxQuota) {
        this.maxQuota = maxQuota;
    }

}
