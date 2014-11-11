/*
 * (C) Copyright 2002-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
