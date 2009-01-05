package org.nuxeo.ecm.core.convert.cache;

import java.io.File;
import java.util.Date;

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

public class ConversionCacheEntry {

    protected Date lastAccessTime = null;
    protected transient BlobHolder bh;
    protected boolean persisted=false;
    protected String persistPath;
    protected long sizeInKB=0;


    public ConversionCacheEntry(BlobHolder bh) {
        this.bh=bh;
        updateAccessTime();
    }

    protected void updateAccessTime() {
        lastAccessTime = new Date();
    }


    public boolean persist(String basePath) throws Exception {

        if (bh instanceof CachableBlobHolder) {
            CachableBlobHolder cbh = (CachableBlobHolder) bh;
            persistPath = cbh.persist(basePath);
            sizeInKB = new File(persistPath).length() / 1024;
            persisted=true;
        }
        bh=null;
        return persisted;
    }

    public void remove() {
        if ((persisted) && (persistPath!=null)) {
            new File(persistPath).delete();
        }
    }

    public BlobHolder restore() {
        updateAccessTime();
        if ((persisted) && (persistPath!=null)) {
            return new SimpleCachableBlobHolder(persistPath);
        }
        else
            return null;
    }


    public long getDiskSpaceUsageInKB() {
        return sizeInKB;
    }

    public Date getLastAccessedTime()
    {
        return lastAccessTime;
    }


}
