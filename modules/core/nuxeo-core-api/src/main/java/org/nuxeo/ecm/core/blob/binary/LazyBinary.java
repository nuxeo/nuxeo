/*
 * (C) Copyright 2011-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.blob.binary;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.runtime.api.Framework;

/**
 * Lazy Binary that fetches its remote stream on first access.
 */
public class LazyBinary extends Binary {

    private static final long serialVersionUID = 1L;

    // transient to be Serializable
    protected transient CachingBinaryManager cbm;

    public LazyBinary(String digest, String repoName, CachingBinaryManager cbm) {
        super(digest, repoName);
        this.cbm = cbm;
    }

    // because the class is Serializable, re-acquire the CachingBinaryManager
    protected CachingBinaryManager getCachingBinaryManager() {
        if (cbm == null) {
            if (blobProviderId == null) {
                throw new UnsupportedOperationException("Cannot find binary manager, no blob provider id");
            }
            BlobManager bm = Framework.getService(BlobManager.class);
            BlobProvider bp = bm.getBlobProvider(blobProviderId);
            cbm = (CachingBinaryManager) bp.getBinaryManager();
        }
        return cbm;
    }

    @Override
    public InputStream getStream() throws IOException {
        File file = getFile();
        return file == null ? null : new FileInputStream(file);
    }

    @Override
    public File getFile() {
        try {
            return getCachingBinaryManager().getFile(digest);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
