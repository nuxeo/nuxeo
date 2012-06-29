package org.nuxeo.ecm.quota.size;

public class QuotaInfo {

    protected final QuotaDisplayValue innerSize;

    protected final QuotaDisplayValue totalSize;

    protected final QuotaDisplayValue maxQuota;

    public QuotaInfo(long innerSize, long totalSize, long maxQuota) {
        this.innerSize = new QuotaDisplayValue(innerSize);
        this.totalSize = new QuotaDisplayValue(totalSize);
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
