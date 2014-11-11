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
 *     anguenot
 *
 * $Id: ModifiedDocumentDescriptor.java 30185 2008-02-14 17:56:36Z tdelprat $
 */

package org.nuxeo.ecm.platform.audit.ws;

import java.io.Serializable;
import java.util.Date;

/**
 * Web service modified document descriptor.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class ModifiedDocumentDescriptor implements Serializable {

    private static final long serialVersionUID = 17654654654L;

    private String modified;

    private String type;

    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public ModifiedDocumentDescriptor() {
        this(null, null, null);
    }

    public ModifiedDocumentDescriptor(Date modified, String type, String uuid) {
        this.modified = modified.toString();
        this.type = type;
        this.uuid = uuid;
    }

    /**
     * Returns the modification date.
     *
     * @return the modification date.
     */
    public String getModified() {
        return modified;
    }

    /**
     * Returns the doc type.
     *
     * @return the doc type.
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the document UUID.
     *
     * @return the document UUID.
     */
    public String getUUID() {
        return uuid;
    }

    public void setModified(String modified) {
        this.modified = modified;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

}
