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
     * Here, we will always return string for the moment. Request from the
     * <i>Intuition</i> team.
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

}
