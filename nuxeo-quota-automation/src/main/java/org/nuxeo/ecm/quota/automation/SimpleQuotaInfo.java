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
