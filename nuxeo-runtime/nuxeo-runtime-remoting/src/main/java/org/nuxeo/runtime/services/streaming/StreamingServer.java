/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.runtime.services.streaming;

import java.io.IOException;



/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface StreamingServer {

    DownloadInfo createDownloadSession(String uri) throws IOException;

    void closeDownloadSession(long sid) throws IOException;


    String createUploadSession() throws IOException;

    void closeUploadSession(String uri) throws IOException;


    boolean hasStream(String uri);

    void removeStream(String uri);

    /**
     * Uploads the given bytes.
     *
     * @param uri the stream uri
     * @param bytes the bytes to upload
     * @throws IOException
     */
    void uploadBytes(String uri, byte[] bytes) throws IOException;

    /**
     * Downloads the next 'size' bytes from the given download session.
     *
     * @param sid the download session
     * @param size the number of bytes to download
     * @return the downloaded byte array or null if no more bytes are available
     * @throws IOException
     */
    byte[] downloadBytes(long sid, int size) throws IOException;

}
