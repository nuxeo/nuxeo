/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

    public InputStream getStream() throws IOException {
        return url.openStream();
    }

    public Blob persist() throws IOException {
        return this;
    }

    public boolean isPersistent() {
        return true;
    }

}
