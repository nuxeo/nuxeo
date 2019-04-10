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
 * Representation of a document diff, schema by schema and field by field.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public interface DocumentDiff extends Serializable {

    /**
     * Gets the doc diff.
     * 
     * @return the doc diff
     */
    Map<String, SchemaDiff> getDocDiff();

    /**
     * Gets the schema count.
     * 
     * @return the schema count
     */
    int getSchemaCount();

    /**
     * Checks if the doc diff is empty.
     * 
     * @return true, if is empty
     */
    boolean isDocDiffEmpty();

    /**
     * Gets the schema names as a list.
     * 
     * @return the schema names
     */
    List<String> getSchemaNames();

    /**
     * Gets the schema diff.
     * 
     * @param schema the schema
     * @return the schema diff
     */
    SchemaDiff getSchemaDiff(String schema);

    /**
     * Inits schema diff.
     * 
     * @param schema the schema
     * @return the map
     */
    SchemaDiff initSchemaDiff(String schema);

}
