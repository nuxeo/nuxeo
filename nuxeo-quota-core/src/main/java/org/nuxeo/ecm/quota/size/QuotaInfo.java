package org.nuxeo.ecm.quota.size;

public class QuotaInfo {

    protected final long innerSize;

    protected final long totalSize;

    protected final long maxQuota;

    public QuotaInfo(long innerSize, long totalSize, long maxQuota) {
        this.innerSize = innerSize;
        this.totalSize = totalSize;
        this.maxQuota = maxQuota;
    }

    public QuotaDisplayValue getInnerSize() {
        return new QuotaDisplayValue(innerSize);
    }

    public QuotaDisplayValue getTotalSize() {
        return new QuotaDisplayValue(totalSize);
    }

    public QuotaDisplayValue getMaxQuota() {
        return new QuotaDisplayValue(maxQuota);
    }

    public float getTotalPercent() {
        if (totalSize == 0 || maxQuota <= 0) {
            return 0;
        }
        return (new Float(totalSize) / maxQuota) * 100;
    }

    public float getInnerPercent() {
        if (innerSize == 0 || maxQuota <= 0) {
            return 0;
        }
        return (new Float(innerSize) / maxQuota) * 100;
    }
}
