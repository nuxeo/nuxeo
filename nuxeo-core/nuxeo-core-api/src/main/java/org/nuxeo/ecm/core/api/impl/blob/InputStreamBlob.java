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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.ecm.core.api.Blob;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class InputStreamBlob extends DefaultStreamBlob {

    private static final long serialVersionUID = 2044137587685886328L;

    protected final InputStream in;

    protected File file;

    public InputStreamBlob(InputStream in) {
        this(in, null, null);
        // TODO try to guess content type from file extension
    }

    public InputStreamBlob(InputStream in, String ctype) {
        this(in, ctype, null);
    }

    public InputStreamBlob(InputStream in, String ctype, String encoding) {
        this.in = in;
        mimeType = ctype;
        this.encoding = encoding;
    }

    public InputStreamBlob(InputStream in, String ctype, String encoding, String filename, String digest) {
        this.in = in;
        mimeType = ctype;
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
