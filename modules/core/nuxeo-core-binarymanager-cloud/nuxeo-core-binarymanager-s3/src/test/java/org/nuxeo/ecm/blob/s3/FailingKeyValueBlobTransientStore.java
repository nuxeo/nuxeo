/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     pierre
 */
package org.nuxeo.ecm.blob.s3;

import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.transientstore.keyvalueblob.KeyValueBlobTransientStore;

/**
 * Mocked {@link KeyValueBlobTransientStore} to test failure on copy.
 *
 * @since 11.1
 */
public class FailingKeyValueBlobTransientStore extends KeyValueBlobTransientStore {

    @Override
    public void putBlobs(String key, List<Blob> blobs) {
        throw new NuxeoException("putBlobs failed");
    }

}
