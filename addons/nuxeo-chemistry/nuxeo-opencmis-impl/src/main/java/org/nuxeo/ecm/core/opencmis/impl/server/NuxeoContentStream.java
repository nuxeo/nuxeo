/*
 * (C) Copyright 2009-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl.server;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.List;

import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.nuxeo.ecm.core.api.Blob;

/**
 * Nuxeo implementation of a CMIS {@link ContentStream}, backed by a
 * {@link Blob}.
 */
public class NuxeoContentStream implements ContentStream {

    protected final Blob blob;

    public NuxeoContentStream(Blob blob) {
        this.blob = blob;
    }

    @Override
    public long getLength() {
        return blob.getLength();
    }

    @Override
    public BigInteger getBigLength() {
        return BigInteger.valueOf(blob.getLength());
    }

    @Override
    public String getMimeType() {
        return blob.getMimeType();
    }

    @Override
    public String getFileName() {
        return blob.getFilename();
    }

    @Override
    public InputStream getStream() {
        try {
            return blob.getStream();
        } catch (IOException e) {
            throw new CmisRuntimeException("Failed to get stream", e);
        }
    }

    @Override
    public List<Object> getExtensions() {
        return null;
    }

    @Override
    public void setExtensions(List<Object> extensions) {
        throw new UnsupportedOperationException();
    }

}
