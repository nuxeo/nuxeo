/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
public interface ExportedDocument {

    /**
     * @return source DocumentLocation
     */
    DocumentLocation getSourceLocation();

    Path getPath();

    void setPath(Path path);

    String getId();

    void setId(String id);

    String getType();

    Document getDocument();

    void setDocument(Document document);

    Map<String, Blob> getBlobs();

    void putBlob(String id, Blob blob);

    Blob removeBlob(String id);

    Blob getBlob(String id);

    boolean hasExternalBlobs();

    Map<String, Document> getDocuments();

    Document getDocument(String id);

    void putDocument(String id, Document doc);

    Document removeDocument(String id);

    /**
     * The number of files describing the document.
     */
    int getFilesCount();

}
