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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.tree;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.Sorter;
import org.nuxeo.ecm.core.search.api.client.querymodel.descriptor.QueryModelDescriptor;

/**
 * Interface for the tree manager service.
 *
 * @author Anahide Tchertchian
 */
public interface TreeManager extends Serializable {

    /**
     * Returns filter to use for given plugin names.
     */
    Filter getFilter(String pluginName);

    /**
     * Returns leaf filter to use for given plugin names.
     */
    Filter getLeafFilter(String pluginName);

    /**
     * Returns sorter to use for given plugin name.
     */
    Sorter getSorter(String pluginName);

    /**
     * Returns the query model descriptor to use for given plugin name.
     */
    QueryModelDescriptor getQueryModelDescriptor(String pluginName);

    /**
     * Returns the query model descriptor to use for given plugin name
     * on an Orderable DocumentModel.
     */
    QueryModelDescriptor getOrderableQueryModelDescriptor(String pluginName);

}
