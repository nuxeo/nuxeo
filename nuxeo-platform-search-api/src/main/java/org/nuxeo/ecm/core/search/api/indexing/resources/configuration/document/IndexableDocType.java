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
 * $Id: IndexableDocType.java 28476 2008-01-04 09:52:52Z sfermigier $
 */

package org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document;

import java.io.Serializable;
import java.util.List;

/**
 * Indexable doc type.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public interface IndexableDocType extends Serializable {

    /**
     * Return the doc type.
     *
     * @return the doc type.
     */
    String getType();

    /**
     * All schemas should be indexed ?
     *
     * @return true if all schemas are indexable.
     */
    boolean areAllSchemasIndexable();

    /**
     * All fields sortable ?
     *
     * @return true if all fields should be sortable unless an explicit
     *         configuration specifies the contrary
     */
    boolean areAllFieldsSortable();

    /**
     * Returns explicit schemas that should be excluded.
     *
     * @return a list of schema names.
     */
    List<String> getExcludedSchemas();

    /**
     * Returns the list of resources to take into consideration while indexing.
     *
     * @return a list of resource names.
     */
    List<String> getResources();

}
