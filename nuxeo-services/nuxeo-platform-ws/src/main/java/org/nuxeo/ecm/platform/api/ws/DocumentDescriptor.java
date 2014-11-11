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

package org.nuxeo.ecm.platform.api.ws;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Web service base document descriptor.
 * <p>
 * The document descriptor contains minimal information about a document as
 * needed by a client to display it and navigate through the repository tree.
 * <p>
 * The descriptor exposes:
 * <ul>
 * <li> the document UUID
 * <li> the document type
 * <li> the document title
 * </ul>
 *
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
        } catch (ClientException e) {
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
