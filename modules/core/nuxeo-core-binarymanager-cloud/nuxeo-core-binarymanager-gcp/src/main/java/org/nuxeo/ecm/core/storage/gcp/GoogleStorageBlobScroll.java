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
package org.nuxeo.ecm.core.storage.gcp;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.nuxeo.ecm.core.blob.scroll.AbstractBlobScroll;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage.BlobField;
import com.google.cloud.storage.Storage.BlobListOption;

/**
 * Scroll files of the Google Storage blob store of a {@link GoogleStorageBlobProvider}, the scroll query is the
 * provider id.
 *
 * @since 2023.5
 */
public class GoogleStorageBlobScroll extends AbstractBlobScroll<GoogleStorageBlobProvider> {

    protected GoogleStorageBlobStore store;

    protected int prefixLength;

    protected Page<Blob> blobs;

    @Override
    protected void init(GoogleStorageBlobProvider provider) {
        this.store = (GoogleStorageBlobStore) provider.store.unwrap();
        this.prefixLength = this.store.bucketPrefix.length();
    }

    @Override
    public boolean hasNext() {
        return blobs == null || blobs.hasNextPage();
    }

    @Override
    public List<String> next() {
        if (blobs == null) {
            blobs = this.store.bucket.list(BlobListOption.fields(BlobField.ID, BlobField.SIZE),
                    BlobListOption.prefix(this.store.bucketPrefix), BlobListOption.pageSize(size));
        } else {
            if (!blobs.hasNextPage()) {
                throw new NoSuchElementException();
            }
            blobs = blobs.getNextPage();
        }
        List<String> result = new ArrayList<>();
        for (Blob blob : blobs.getValues()) {
            addTo(result, blob.getName().substring(prefixLength), () -> blob.getSize());
        }
        return result;
    }

}
