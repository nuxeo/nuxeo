/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.ui.tree;

import java.io.Serializable;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface ContentProvider extends Serializable {

    /**
     * Gets the name of the object.
     * <p>
     * The name must be an unique identifier relative to the parent item.
     * It will be used as node names in the tree so that they will construct the item path.
     *
     * @param obj the object
     * @return the name
     */
    String getName(Object obj);

    /**
     * Gets the label to be used when displaying the given object.
     *
     * @param obj the object
     * @return the label
     */
    String getLabel(Object obj);

    /**
     * Gets the object facets.
     * <p>
     * Facets are arbitrary strings that should describe object capabilities and
     * can be used to decorate later the item.
     * <p>
     * In a web environment they may be translated to CSS classes.
     *
     * @return item facets
     */
    String[] getFacets(Object object);

    /**
     * Whether the given object may have children (e.g it's a container).
     *
     * @param obj the object to test
     * @return true if it may have children, false otherwise
     */
    boolean isContainer(Object obj);

    /**
     * Gets the top level items.
     * <p>
     * The items will be shown on the top level of the tree.
     * These items are computed from the tree input that will be considered the tree root.
     * The tree root is not visible.
     *
     * @param input the tree view input
     * @return the top level items
     */
    Object[] getElements(Object input);

    /**
     * Gets the children for the given object.
     * <p>
     * This method is used to populate the nested branches of the tree.
     *
     * @param obj the object
     * @return the children or null if no children are supported
     */
    Object[] getChildren(Object obj);

}
