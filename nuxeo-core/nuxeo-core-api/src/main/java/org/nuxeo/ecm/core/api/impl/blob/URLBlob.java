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
import java.net.URL;

/**
 * Blob backed by a URL. Its length is -1. Note that the encoding is not detected even for an HTTP URL.
 */
public class URLBlob extends AbstractBlob implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final URL url;

    public URLBlob(URL url) {
        this(url, null, null);
    }

    public URLBlob(URL url, String mimeType) {
        this(url, mimeType, null);
    }

    public URLBlob(URL url, String mimeType, String encoding) {
        if (url == null) {
            throw new NullPointerException("null url");
        }
        this.url = url;
        this.mimeType = mimeType;
        this.encoding = encoding;
    }

    /**
     * @deprecated since 7.2, use a separate {@link #setFilename} call
     */
    @Deprecated
    public URLBlob(URL url, String mimeType, String encoding, String filename) {
        this(url, mimeType, encoding);
        this.filename = filename;
    }

    @Override
    public InputStream getStream() throws IOException {
        return url.openStream();
    }

}
