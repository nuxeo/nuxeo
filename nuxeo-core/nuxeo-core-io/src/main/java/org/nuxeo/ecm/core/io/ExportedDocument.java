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
 *     bstefanescu
 *
 * $Id: ExportedDocument.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.ecm.core.io;

import java.util.Map;

import org.dom4j.Document;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentLocation;

/**
 * A representation for an exported document.
 * <p>
 * It contains all the information needed to restore document data and state.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@SuppressWarnings("unchecked")
public interface ExportedDocument {

    /**
     * @return source DocumentLocation
     */
    DocumentLocation getSourceLocation();

    /**
     * @return the path.
     */
    Path getPath();

    /**
     * @param path the path to set.
     */
    void setPath(Path path);

    /**
     * @return the id.
     */
    String getId();

    /**
     * @param id the id to set.
     */
    void setId(String id);

    String getType();

    /**
     * @return the document.
     */
    Document getDocument();

    /**
     * @param document the document to set.
     */
    void setDocument(Document document);

    /**
     * @return the blobs.
     */
    Map<String, Blob> getBlobs();

    void putBlob(String id, Blob blob);

    Blob removeBlob(String id);

    Blob getBlob(String id);

    boolean hasExternalBlobs();

    /**
     * @return the documents.
     */
    Map<String, Document> getDocuments();

    Document getDocument(String id);

    void putDocument(String id, Document doc);

    Document removeDocument(String id);

    /**
     * The number of files describing the document.
     *
     * @return
     */
    int getFilesCount();

}
