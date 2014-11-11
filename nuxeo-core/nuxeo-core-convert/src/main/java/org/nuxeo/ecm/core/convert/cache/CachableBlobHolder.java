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
 */
package org.nuxeo.ecm.core.convert.cache;

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

/**
 * Extended interface for {@link BlobHolder} that can be cached.
 * <p>
 * A BlobHolder can be cached if it can be persisted to disk and reloaded from a
 * file. Converters need to return BlobHolders that implement this interface to
 * make the result cachable.
 *
 * @author tiry
 */
public interface CachableBlobHolder extends BlobHolder {

    /**
     * Persists the blobHolder to disk.
     *
     * @param basePath
     *            the base path (existing directory) as determined by the caller
     * @return the full path of the newly created FileSystem resource
     * @throws Exception
     */
    String persist(String basePath) throws Exception;

    /**
     * Reloads the {@link BlobHolder} from a file.
     *
     * @param path
     */
    void load(String path);

}
