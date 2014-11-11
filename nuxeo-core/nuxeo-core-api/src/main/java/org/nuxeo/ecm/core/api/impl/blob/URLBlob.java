/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.impl.blob;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.nuxeo.ecm.core.api.Blob;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class URLBlob extends DefaultStreamBlob {

    private static final long serialVersionUID = 8153160000788820352L;

    protected final URL url;


    public URLBlob(URL url) {
        this (url, null, null);
    }

    public URLBlob(URL url, String ctype) {
        this (url, ctype, null);
    }

    public URLBlob(URL url, String ctype, String encoding) {
        this (url, ctype, encoding, null, null);
    }

    public URLBlob(URL url, String ctype, String encoding, String filename, String digest) {
        this.url = url;
        mimeType = ctype;
        this.encoding = encoding;
        this.filename = filename;
        this.digest = digest;
    }

    @Override
    public InputStream getStream() throws IOException {
        return url.openStream();
    }

    @Override
    public Blob persist() throws IOException {
        return this;
    }

    @Override
    public boolean isPersistent() {
        return true;
    }

}
