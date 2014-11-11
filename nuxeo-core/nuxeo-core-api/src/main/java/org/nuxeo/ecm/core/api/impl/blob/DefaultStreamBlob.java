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
