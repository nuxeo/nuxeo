package org.nuxeo.ecm.core.convert.cache;

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

public interface CachableBlobHolder extends BlobHolder {

    public String persist(String basePath) throws Exception;

    public void load(String path);

}
