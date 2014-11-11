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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: FieldDefinition.java 26053 2007-10-16 01:45:43Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.api;

import java.io.Serializable;

/**
 * Interface for field definition.
 * <p>
 * Will help to identify a document field.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface FieldDefinition extends Serializable {

    /**
     * Optional schema name.
     * <p>
     * If the field is used to identify a sub field within a complex type, no
     * schema name is needed. Otherwise, the field will not be resolved
     * correctly if the property does not have a prefix.
     */
    String getSchemaName();

    /**
     * Returns the field name, following XPath conventions.
     * <p>
     * If the field is prefixed, it should contain the prefix followed by ':'.
     * Examples: dc:title, dc:author/name
     */
    String getFieldName();

    /**
     * Returns the computed property name with schema and field information.
     */
    String getPropertyName();

}
