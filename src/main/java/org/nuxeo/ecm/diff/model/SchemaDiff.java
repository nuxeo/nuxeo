/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     ataillefer
 */
package org.nuxeo.ecm.diff.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Representation of a schema diff, field by field.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public interface SchemaDiff extends Serializable {

    /**
     * Gets the schema diff.
     * 
     * @return the schema diff
     */
    Map<String, PropertyDiff> getSchemaDiff();

    /**
     * Gets the field count.
     * 
     * @return the field count
     */
    int getFieldCount();

    /**
     * Gets the field names as a list.
     * 
     * @return the field names
     */
    List<String> getFieldNames();

    /**
     * Gets the field diff.
     * 
     * @param field the field
     * @return the field diff
     */
    PropertyDiff getFieldDiff(String field);

    /**
     * Put field diff.
     * 
     * @param field the field
     * @param fieldDiff the field diff
     * @return the property diff
     */
    PropertyDiff putFieldDiff(String field, PropertyDiff fieldDiff);

}
