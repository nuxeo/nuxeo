/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.nuxeo.mail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

import org.nuxeo.ecm.core.api.Blob;

/**
 * Wraps a {@link Blob} into a {@link DataSource}.
 *
 * @since 2023.3
 */
public record BlobDataSource(Blob blob) implements DataSource {

    @Override
    public String getContentType() {
        return blob.getMimeType();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return blob.getStream();
    }

    @Override
    public String getName() {
        return blob.getFilename();
    }

    @Override
    public OutputStream getOutputStream() {
        throw new UnsupportedOperationException("BlobDataSource is read-only");
    }
}
