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

import org.nuxeo.ecm.core.api.Blob;

/**
 * Blob based on an {@link InputStream}.
 * <p>
 * The method {@link #getStream} will return the original stream and therefore can only be consumed once.
 */
public class InputStreamBlob extends AbstractBlob {

    protected InputStream in;

    public InputStreamBlob(InputStream in) {
        this(in, null, null);
    }

    public InputStreamBlob(InputStream in, String mimeType) {
        this(in, mimeType, null);
    }

    public InputStreamBlob(InputStream in, String mimeType, String encoding) {
        this(in, mimeType, encoding, null, null);
    }

    public InputStreamBlob(InputStream in, String mimeType, String encoding, String filename) {
        this(in, mimeType, encoding, filename, null);
    }

    public InputStreamBlob(InputStream in, String mimeType, String encoding, String filename, String digest) {
        this.in = in;
        this.mimeType = mimeType;
        this.encoding = encoding;
        this.filename = filename;
        this.digest = digest;
    }

    @Override
    public InputStream getStream() {
        return in;
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public Blob persist() throws IOException {
        return new FileBlob(in, mimeType, encoding);
    }

}
