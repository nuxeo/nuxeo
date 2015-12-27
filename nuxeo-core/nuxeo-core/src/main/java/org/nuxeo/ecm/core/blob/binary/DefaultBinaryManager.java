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
 *     Florent Guillaume, jcarsique
 */

package org.nuxeo.ecm.core.blob.binary;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.output.NullOutputStream;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;

/**
 * A simple filesystem-based binary manager. It stores the binaries according to their digest (hash), which means that
 * no transactional behavior needs to be implemented.
 * <p>
 * A garbage collection is needed to purge unused binaries.
 * <p>
 * The format of the <em>binaries</em> directory is:
 * <ul>
 * <li><em>data/</em> hierarchy with the actual binaries in subdirectories,</li>
 * <li><em>tmp/</em> temporary storage during creation,</li>
 * <li><em>config.xml</em> a file containing the configuration used.</li>
 * </ul>
 *
 * @author Florent Guillaume
 */
public class DefaultBinaryManager extends LocalBinaryManager {

    @Override
    public Binary getBinary(Blob blob) throws IOException {
        if (!(blob instanceof FileBlob) || !((FileBlob) blob).isTemporary()) {
            return super.getBinary(blob); // just open the stream
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
        File digestFile = getFileForDigest(digest, true);
        if (digestFile.exists()) {
            // The file with the proper digest is already there so don't do anything. This is to avoid
            // "Stale NFS File Handle" problems which would occur if we tried to overwrite it anyway.
            // Note that this doesn't try to protect from the case where two identical files are uploaded
            // at the same time.
            // Update date for the GC.
            digestFile.setLastModified(blob.getFile().lastModified());
        } else {
            blob.moveTo(digestFile);
        }
        return digest;
    }

}
