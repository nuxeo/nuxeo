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

import java.io.Serializable;


/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class DefaultStreamBlob extends StreamBlob implements Serializable {

    private static final long serialVersionUID = -5714134759770781321L;

    protected String digest;
    protected String filename;
    protected String encoding;
    protected String mimeType;

    @Override
    public String getDigest() {
        return digest;
    }

    @Override
    public String getEncoding() {
        return encoding;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public void setDigest(String digest) {
        this.digest = digest;
    }

    @Override
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    @Override
    public void setFilename(String filename) {
        this.filename = filename;
    }

    @Override
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

}
