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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.nuxeo.ecm.core.api.Blob;

/**
 * A {@link Blob} backed by an entry in a ZIP file.
 *
 * @since 7.2
 */
public class ZipEntryBlob extends AbstractBlob implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final ZipFile zipFile;

    protected final ZipEntry zipEntry;

    /**
     * Creates a {@link Blob} from an entry in a zip file. The {@link ZipFile} must not be closed until the stream has
     * been read.
     *
     * @param zipFile the zip file
     * @param zipEntry the zip entry
     */
    public ZipEntryBlob(ZipFile zipFile, ZipEntry zipEntry) {
        this.zipFile = zipFile;
        this.zipEntry = zipEntry;
    }

    @Override
    public InputStream getStream() throws IOException {
        return zipFile.getInputStream(zipEntry);
    }

    @Override
    public long getLength() {
        return zipEntry.getSize();
    }

}
