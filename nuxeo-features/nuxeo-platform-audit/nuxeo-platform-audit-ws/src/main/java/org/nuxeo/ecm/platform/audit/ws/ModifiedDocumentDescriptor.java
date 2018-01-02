/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
