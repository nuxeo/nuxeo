/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
