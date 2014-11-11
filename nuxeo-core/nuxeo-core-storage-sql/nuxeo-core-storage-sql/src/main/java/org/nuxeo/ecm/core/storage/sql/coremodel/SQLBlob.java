/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.DefaultStreamBlob;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.storage.sql.Binary;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.streaming.StreamSource;

/**
 * A {@link Blob} wrapping a {@link Binary} value.
 *
 * @author Florent Guillaume
 * @author Bogdan Stefanescu
 */
public class SQLBlob extends DefaultStreamBlob implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * By default the SQLBlob is remotable through Nuxeo streaming service.
     * You can disable this by defining the following runtime (or system) property:
     * <code>org.nuxeo.ecm.core.storage.sql.blob_streaming = false</code>
     * This way the blob will use the default serialization (file serialization) that is optimized for
     * servers that are using a shared file system (and not nuxeo streaming)
     */
    public static final boolean IS_STREAMING_ENABLED = Boolean.parseBoolean(
            Framework.getProperty("org.nuxeo.ecm.core.storage.sql.blob_streaming", "true"));

    protected final Binary binary;

    public SQLBlob(Binary binary, String filename, String mimeType,
            String encoding, String digest) {
        this.binary = binary;
        setFilename(filename);
        setMimeType(mimeType);
        setEncoding(encoding);
        setDigest(digest);
    }

    @Override
    public long getLength() {
        return binary.getLength();
    }

    @Override
    public InputStream getStream() throws IOException {
        return binary.getStream();
    }

    @Override
    public boolean isPersistent() {
        return true;
    }

    @Override
    public Blob persist() {
        return this;
    }

    public Binary getBinary() {
        return binary;
    }

    /**
     * Replace this object with a {@link StreamingBlob} when serialized.
     * The StreamingBlob object can be sent to remote machines through nuxeo streaming mechanism.
     * If IS_STREAMING_ENABLED is false then no replace takes place.
     *
     * @return a streaming blob that points to the same content as this one
     * @throws ObjectStreamException
     */
    public Object writeReplace() throws ObjectStreamException {
        if (IS_STREAMING_ENABLED) {
            StreamSource src = binary.getStreamSource();
            return new StreamingBlob(src, getMimeType(), getEncoding(), getFilename(), getDigest());
        } else {
            return this;
        }
    }

}
