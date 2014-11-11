/*
 * (C) Copyright 2011-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.binary;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.streaming.FileSource;
import org.nuxeo.runtime.services.streaming.StreamSource;

/**
 * Lazy Binary that fetches its remote stream on first access.
 */
public class LazyBinary extends Binary {

    private static final long serialVersionUID = 1L;

    protected boolean hasLength;

    // transient to be Serializable
    protected transient CachingBinaryManager cbm;

    public LazyBinary(String digest, String repoName, CachingBinaryManager cbm) {
        super(digest, repoName);
        this.cbm = cbm;
    }

    // because the class is static, re-acquire the CachingBinaryManager
    protected CachingBinaryManager getCachingBinaryManager() {
        if (cbm == null) {
            if (repoName == null) {
                throw new UnsupportedOperationException(
                        "Cannot find binary manager, no repository name");
            }
            BinaryManagerService bms = Framework.getLocalService(BinaryManagerService.class);
            cbm = (CachingBinaryManager) bms.getBinaryManager(repoName);
        }
        return cbm;
    }

    @Override
    public InputStream getStream() throws IOException {
        if (file == null) {
            file = getCachingBinaryManager().getFile(digest);
            if (file != null) {
                length = file.length();
                hasLength = true;
            }
        }
        if (file == null) {
            return null;
        } else {
            return new FileInputStream(file);
        }
    }

    @Override
    public StreamSource getStreamSource() {
        if (file == null) {
            try {
                file = getCachingBinaryManager().getFile(digest);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (file != null) {
                length = file.length();
                hasLength = true;
            }
        }
        return file == null ? null : new FileSource(file);
    }

    @Override
    public long getLength() {
        if (!hasLength) {
            Long len;
            try {
                len = getCachingBinaryManager().getLength(digest);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            length = len == null ? 0 : len.longValue();
            hasLength = true;
        }
        return length;
    }

}
