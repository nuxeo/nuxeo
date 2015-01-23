/*
 * Copyright (c) 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
