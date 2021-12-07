/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Andre Justo
 */
package org.nuxeo.binary.metadata.test;

import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.AbstractBlobProvider;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.ManagedBlob;

public class MockDummyBlobProvider extends AbstractBlobProvider {

    @Override
    public boolean supportsSync() {
        return supportsUserUpdate();
    }

    @Override
    public InputStream getStream(ManagedBlob blob) throws IOException {
        return getClass().getResourceAsStream("/data/" + blob.getFilename());
    }

    @Override
    public Blob readBlob(BlobInfo blobInfo) throws IOException {
        return null;
    }

    @Override
    public String writeBlob(Blob blob) throws IOException {
        return null;
    }

    @Override
    public void close() {
    }

}
