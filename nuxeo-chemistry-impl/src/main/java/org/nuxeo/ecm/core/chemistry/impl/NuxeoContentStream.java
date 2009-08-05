/*
 * Copyright 2009 Nuxeo SA <http://nuxeo.com>
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
 * Authors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.chemistry.impl;

import java.io.IOException;
import java.io.InputStream;

import org.apache.chemistry.ContentStream;
import org.nuxeo.ecm.core.api.Blob;

public class NuxeoContentStream implements ContentStream {

    protected final Blob blob;

    public NuxeoContentStream(Blob blob) {
        this.blob = blob;
    }

    public long getLength() {
        return blob.getLength();
    }

    public String getMimeType() {
        return blob.getMimeType();
    }

    public String getFileName() {
        return blob.getFilename();
    }

    public InputStream getStream() throws IOException {
        return blob.getStream();
    }

}
