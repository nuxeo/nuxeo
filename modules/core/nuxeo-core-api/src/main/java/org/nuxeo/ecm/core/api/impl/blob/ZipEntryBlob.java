/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.impl.blob;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.nuxeo.ecm.core.api.Blob;

/**
 * A {@link Blob} backed by an entry in a ZIP file.
 *
 * @since 7.2
 */
public class ZipEntryBlob extends AbstractBlob implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final ZipFile zipFile;

    protected final ZipEntry zipEntry;

    /**
     * Creates a {@link Blob} from an entry in a zip file. The {@link ZipFile} must not be closed until the stream has
     * been read.
     *
     * @param zipFile the zip file
     * @param zipEntry the zip entry
     */
    public ZipEntryBlob(ZipFile zipFile, ZipEntry zipEntry) {
        this.zipFile = zipFile;
        this.zipEntry = zipEntry;
    }

    @Override
    public InputStream getStream() throws IOException {
        return zipFile.getInputStream(zipEntry);
    }

    @Override
    public long getLength() {
        return zipEntry.getSize();
    }

}
