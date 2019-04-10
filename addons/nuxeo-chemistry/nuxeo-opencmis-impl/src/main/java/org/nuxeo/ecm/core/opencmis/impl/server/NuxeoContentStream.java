/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl.server;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.List;

import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;
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
    public List<CmisExtensionElement> getExtensions() {
        return null;
    }

    @Override
    public void setExtensions(List<CmisExtensionElement> extensions) {
        throw new UnsupportedOperationException();
    }

}
