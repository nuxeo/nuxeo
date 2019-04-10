/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.core.api.filesystem;

import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

/**
 * The Filename Service exposes all the operations needed to create or keep in
 * sync the filename, dc:title and short name of a document:
 * <ul>
 * <li>the filename is that of the main attached file ("file" schema), used when
 * a file is downloaded,</li>
 * <li>the title is the dc:title property, used in the UI,</li>
 * <li>the short name is doc.getName(), used as a path segment in the permanent
 * URL.</li>
 * </ul>
 * <p>
 * Filename, title and name are used by various services that need to expose
 * Nuxeo documents to external filesystem-like services: CMIS, WebDAV, WSS,
 * which most of the time expect them to be in sync.
 *
 * @since 5.4.1
 */
public interface FilesystemService {

    /**
     * Property for the main file blob.
     */
    String FILE_PROPERTY = "file:content";

    /**
     * Property for the title.
     */
    String TITLE_PROPERTY = "dc:title";

    /**
     * Gets a document's filename.
     *
     * @param doc the document
     * @return the filename, or {@code null} if there is no file
     */
    String getFilename(DocumentModel doc) throws ClientException;

    /**
     * Gets a document's title.
     *
     * @param doc the document
     * @return the title, or {@code null}
     */
    String getTitle(DocumentModel doc) throws ClientException;

    /**
     * Gets a document's name (relative path segment).
     *
     * @param doc the document
     * @return the name
     */
    String getName(DocumentModel doc) throws ClientException;

    /**
     * Gets the document at a given path.
     *
     * @param path the path
     * @return the document, or {@code null} if not found
     */
    public DocumentModel resolvePath(CoreSession session, String path);

    /** used? */
    public Blob resolveBlobPath(CoreSession session, String path);

    public List<String> getChildrenPaths(CoreSession session, String path);

    /** return path of new doc and title */
    public void getCreationInfo(CoreSession session, String folderPath,
            Blob blob);

    public BlobHolder getBlobHolder(CoreSession session, String path);

    /**
     * Sets the filename or title or name (or several of them) on a document.
     * <p>
     * The main filename, {@code dc:title} and name may be changed.
     * <p>
     * If the document contains no main file, then the filename is absent and
     * not changed.
     *
     * @param doc the document
     * @param filename the new filename, or {@code null}
     * @param title the new title, or {@code null}
     * @param oldFilename the previous filename
     * @param name the new name, or {@code null}
     */
    void set(DocumentModel doc, String filename, String title, String name,
            String oldFilename) throws ClientException;

    /**
     * Fixes a document whose filename, title or name may have been updated.
     * <p>
     * When known, the previous version of the document is also passed.
     *
     * @param doc the document
     * @param oldDoc the old document, or {@code null}
     */
    void set(DocumentModel doc, DocumentModel oldDoc) throws ClientException;

}
