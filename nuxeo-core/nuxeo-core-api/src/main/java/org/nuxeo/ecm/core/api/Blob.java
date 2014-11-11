/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

/**
 * A blob contains usually large data.
 * <p>
 * Document fields holding Blob data are by default fetched in a lazy manner.
 * <p>
 * A Blob object hides the data source and it also describes data properties
 * like the encoding or mime-type.
 * <p>
 * The encoding is used to decode Unicode text content that was stored in an
 * encoded form. If not encoding is specified, the default java encoding is
 * used. The encoding is ignored for binary content.
 * <p>
 * When retrieving the content from a document, it will be returned as source
 * content instead of returning the content bytes.
 * <p>
 * The same is true when setting the content for a document: you set a content
 * source and not directly the content bytes. Ex:
 * <code><pre>
 * File file = new File("/tmp/index.html");
 * FileBlob fb = new FileBlob(file);
 * fb.setContentType("text/html");
 * fb.setEncoding("UTF-8"); // this specifies that content bytes will be stored as UTF-8
 * document.setContent(fb);
 * </pre></code>
 * Then you may want to retrieve the content as follow:
 * <code><pre>
 * Blob blob = document.getContent();
 * htmlDoc = blob.getString(); // the content is decoded from UTF-8 into a java string
 * </pre></code>
 *
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface Blob {

    /**
     * Gets the data length in bytes if known.
     *
     * @return the data length or -1 if not known
     */
    long getLength();

    String getEncoding();

    String getMimeType();

    String getFilename();

    String getDigest();

    void setDigest(String digest);

    void setFilename(String filename);

    void setMimeType(String mimeType);

    void setEncoding(String encoding);

    InputStream getStream() throws IOException;

    Reader getReader() throws IOException;

    byte[] getByteArray() throws IOException;

    String getString() throws IOException;

    void transferTo(OutputStream out) throws IOException;

    void transferTo(Writer out) throws IOException;

    void transferTo(File file) throws IOException;

    /**
     * Persist this stream so that getStream() method can be called successfully
     * several times. The persistence is done in a temp file or in memory - this
     * is up to the implementation.
     * <p>
     * Blobs that are already persistent return themselves.
     * <p>
     * Persistence should update the internal structure of the Blob to make it
     * persistent whenever possible and hence return itself whenever possible.
     * This behavior cannot be guaranteed by every implementation however.
     *
     * @return a persistent version of the blob
     * @throws IOException
     */
    Blob persist() throws IOException;

    /**
     * Checks whether this blob is persistent. (i.e. if {@code getStream()} can be
     * successfully called several times).
     *
     * @return true if persistent, false otherwise
     */
    boolean isPersistent();

}
