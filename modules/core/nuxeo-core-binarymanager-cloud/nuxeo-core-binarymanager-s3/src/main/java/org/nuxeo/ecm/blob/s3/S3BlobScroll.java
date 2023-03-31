/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.blob.s3;

import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.DELIMITER;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.nuxeo.ecm.core.blob.scroll.AbstractBlobScroll;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * Scroll objects of the s3 blob store of a #{@link S3BlobProvider}, the scroll query is the provider id.
 *
 * @since 2023
 */
public class S3BlobScroll extends AbstractBlobScroll<S3BlobProvider> {

    protected AmazonS3 amazonS3;

    protected S3BlobStoreConfiguration config;

    protected ObjectListing list;

    protected ListObjectsRequest listObjectsRequest;

    protected S3BlobStore store;

    @Override
    public void init(S3BlobProvider s3BlobProvider) {
        list = null;
        this.store = (S3BlobStore) s3BlobProvider.store.unwrap();
        this.config = this.store.config;
        this.amazonS3 = this.store.amazonS3;
        listObjectsRequest = new ListObjectsRequest().withBucketName(this.store.bucketName)
                                                     .withPrefix(this.store.bucketPrefix);
        listObjectsRequest.withMaxKeys(size);
        if (config.getSubDirsDepth() == 0) {
            // optimization in case of flat hierarchy to do not list sub folders content
            listObjectsRequest.setDelimiter(DELIMITER);
        }
    }

    @Override
    public boolean hasNext() {
        return list == null || list.isTruncated();
    }

    @Override
    public List<String> next() {
        if (list == null) {
            list = amazonS3.listObjects(listObjectsRequest);
        } else {
            if (!list.isTruncated()) {
                throw new NoSuchElementException();
            }
            list = amazonS3.listNextBatchOfObjects(list);
        }
        List<String> result = new ArrayList<>();
        for (S3ObjectSummary summary : list.getObjectSummaries()) {
            String path = summary.getKey().substring(store.bucketPrefix.length());
            // if sub dir depth is greater than 0, it means we have a path strategy in place
            String key = config.getSubDirsDepth() == 0 ? path : store.pathStrategy.getKeyForPath(path);
            if (key == null) {
                continue;
            }
            addTo(result, key, () -> summary.getSize());
        }
        return result;
    }

}
