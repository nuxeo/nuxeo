/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.api.ws;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;

/**
 * Web service base document descriptor.
 * <p>
 * The document descriptor contains minimal information about a document as needed by a client to display it and
 * navigate through the repository tree.
 * <p>
 * The descriptor exposes:
 * <ul>
 * <li>the document UUID
 * <li>the document type
 * <li>the document title
 * </ul>
 * <p>
 * In the case of version document versions the title is the version label
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DocumentDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String title;

    private String type;

    /**
     * Emtpy ctor needed by tools like jaxb.
     */
    public DocumentDescriptor() {
    }

    public DocumentDescriptor(DocumentModel doc) {
        try {
            title = (String) doc.getProperty("dublincore", "title");
        } catch (PropertyException e) {
            title = null;
        }
        if (title == null) {
            title = doc.getName();
        }
        id = doc.getId();
        type = doc.getType();
    }

    public DocumentDescriptor(DocumentModel doc, String title) {
        this.title = title;
        id = doc.getId();
        type = doc.getType();
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public String getUUID() {
        return id;
    }

    /**
     * @param title the title to set.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @param uuid the uuid to set.
     */
    public void setUUID(String uuid) {
        id = uuid;
    }

    /**
     * @param type the type to set.
     */
    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
