/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.blob.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface BlobResource {

    /**
     * Get the blob hash. This is the fingerprint of the blob and should be used to
     * detect blob duplications.
     *
     * @return the MD5 hash of the blob
     */
    String getHash();

    /**
     * The blob resource last modification time.
     * @return the last modification time
     */
    long lastModified();

    /**
     * Get the blob stream
     * @return the input stream
     */
    InputStream getStream() throws IOException;

}
