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
 * $Id: DocumentProperty.java 13220 2007-03-03 18:45:30Z bstefanescu $
 */

package org.nuxeo.ecm.platform.api.ws;

import java.io.Serializable;

/**
 * Web service document property wrapper.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class DocumentProperty implements Serializable {

    private static final long serialVersionUID = -5495522067864308283L;

    private String name;

    private String value;

    /**
     * Empty ctor needed by tools like jaxb.
     */
    public DocumentProperty() {
    }

    public DocumentProperty(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Returns the field name.
     *
     * @return the field name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the field value as a string.
     * <p>
     * Here, we will always return string for the moment. Request from the <i>Intuition</i> team.
     *
     * @return the field value as a string
     */
    public String getValue() {
        return value;
    }

    /**
     * @param name the name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param value the value to set.
     */
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return name + ":" + value;
    }
}
