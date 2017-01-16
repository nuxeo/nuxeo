package org.nuxeo.ecm.core.storage.sql;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.nuxeo.ecm.blob.AbstractTestCloudBinaryManager;

import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public abstract class AbstractS3BinaryTest<T extends S3BinaryManager> extends AbstractTestCloudBinaryManager<T> {

    @Override
    protected Set<String> listObjects() {
        Set<String> digests = new HashSet<>();
        ObjectListing list = null;
        do {
            if (list == null) {
                list = binaryManager.amazonS3.listObjects(binaryManager.bucketName, binaryManager.bucketNamePrefix);
            } else {
                list = binaryManager.amazonS3.listNextBatchOfObjects(list);
            }
            int prefixLength = binaryManager.bucketNamePrefix.length();
            for (S3ObjectSummary summary : list.getObjectSummaries()) {
                String digest = summary.getKey().substring(prefixLength);
                if (!S3BinaryManager.isMD5(digest)) {
                    continue;
                }
                digests.add(digest);
            }
        } while (list.isTruncated());
        return digests;
    }

    /**
     * Removes all objects in the bucket, not only those under the configured prefix.
     */
    @Override
    protected void removeObjects() throws IOException {
        listAllObjects().forEach(key -> binaryManager.amazonS3.deleteObject(binaryManager.bucketName, key));
    }

    /**
     * Lists all objects in the bucket, not only those under the configured prefix.
     */
    protected Set<String> listAllObjects() {
        Set<String> digests = new HashSet<>();
        ObjectListing list = null;
        do {
            if (list == null) {
                list = binaryManager.amazonS3.listObjects(binaryManager.bucketName);
            } else {
                list = binaryManager.amazonS3.listNextBatchOfObjects(list);
            }
            for (S3ObjectSummary summary : list.getObjectSummaries()) {
                String digest = summary.getKey();
                digests.add(digest);
            }
        } while (list.isTruncated());
        return digests;
    }
}
