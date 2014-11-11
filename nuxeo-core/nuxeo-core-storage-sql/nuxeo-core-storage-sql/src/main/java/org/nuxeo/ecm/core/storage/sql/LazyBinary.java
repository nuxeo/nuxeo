/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.runtime.services.streaming.FileSource;
import org.nuxeo.runtime.services.streaming.StreamSource;

/**
 * Base class for a lazy Binary that fetches its remote stream on first access.
 * <p>
 * The methods {@link #cache()}, {@link #fetchFile} and {@link #fetchLength} must be implemented
 * by the concrete class.
 */
public class LazyBinary extends Binary {

    private static final long serialVersionUID = 1L;

    protected boolean hasLength;

    protected transient BinaryFileCache cache;

    /**
     * A lazy binary for the given digest.
     *
     * @param digest the digest for the binary
     * @param fileCache() a file cache used by the length-caching methods
     */
    public LazyBinary(String digest, BinaryFileCache cache, String repo) {
        super(digest, repo);
        this.cache = cache;
    }

    protected BinaryFileCache cache() {
        if (cache == null) {
            if (repoName == null) {
                throw new UnsupportedOperationException("Cannot retrieve file cache, no repository name given");
            }
            BinaryCachingManager mgr = (BinaryCachingManager)RepositoryResolver.getBinaryManager(repoName);
            cache = mgr.fileCache();
        }
        return cache;
    }

    @Override
    public InputStream getStream() throws IOException {
        if (file == null) {
            file = cache().getFile(digest);
            if (file != null) {
                length = file.length();
                hasLength = true;
            }
        }
        return file == null ? null : new FileInputStream(file);
    }

    @Override
    public StreamSource getStreamSource() {
        if (file == null) {
            file = cache().getFile(digest);
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
            Long len = cache().getLength(digest);
            length = len == null ? 0 : len.longValue();
            hasLength = true;
        }
        return length;
    }


}
