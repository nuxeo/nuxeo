package org.nuxeo.ecm.quota.size;

public class BlobSizeInfo {

    protected long blobSize = 0;

    protected long blobSizeDelta = 0;

    public long getBlobSize() {
        return blobSize;
    }

    public long getBlobSizeDelta() {
        return blobSizeDelta;
    }

    public boolean changed() {
        return blobSizeDelta != 0;
    }

    public BlobSizeInfo invert() {
        BlobSizeInfo inverse = new BlobSizeInfo();
        inverse.blobSize = blobSize;
        inverse.blobSizeDelta = -blobSizeDelta;
        return inverse;
    }

    public BlobSizeInfo removeValue() {
        BlobSizeInfo inverse = new BlobSizeInfo();
        inverse.blobSize = blobSize;
        inverse.blobSizeDelta = -blobSize;
        return inverse;
    }
}
