/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 */

package org.nuxeo.ecm.core.blob.binary;

import org.apache.commons.io.output.NullOutputStream;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * A fake binarystore that does not save or remove any binaries.
 *
 * It returns existing blob if present otherwhise a fake content.
 *
 * The only purpose of this binary manager is for benchmarking very large amount of document without requiring an
 * infinite disk space. It also limit the disk IO so we can focus on other bottleneck.
 *
 */
public class FakeBinaryManager extends LocalBinaryManager {

    @Override
    public Binary getBinary(String digest) {
        File file = super.getFileForDigest(digest, false);
        if (file == null || !file.exists()) {
            return new Binary(null, digest, blobProviderId);
        }
        return new Binary(file, digest, blobProviderId);
    }

    @Override
    public Binary getBinary(Blob blob) throws IOException {
        if (!(blob instanceof FileBlob) || !((FileBlob) blob).isTemporary()) {
            return new Binary(null, null, blobProviderId);
        }
        String digest = storeAndDigest((FileBlob) blob);
        File file = getFileForDigest(digest, false);
        /*
         * Now we can build the Binary.
         */
        return new Binary(file, digest, blobProviderId);
    }

    /**
     * Stores and digests a temporary FileBlob.
     */
    protected String storeAndDigest(FileBlob blob) throws IOException {
        String digest;
        try (InputStream in = blob.getStream()) {
            digest = storeAndDigest(in, NullOutputStream.NULL_OUTPUT_STREAM);
        }
        return digest;
    }

}
