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

import org.nuxeo.common.utils.Path;


/**
 * A tree view manage a tree structure of items.
 * The tree data is lazy loaded by using the data provider specified at tree view creation.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface TreeView extends Serializable {

    /**
     * Get the content provider used by this tree
     * @return
     */
    ContentProvider getContentProvider();

    /**
     * Set the content provider to be used by this tree
     * @param provider
     */
    void setContentProvider(ContentProvider provider);

    /**
     * Set the input data
     *
     * @param input. may be null
     */
    void setInput(Object input);

    /**
     * Get the current input of the tree
     * @return the tree input data. may be null.
     */
    Object getInput();

    /**
     * Get the trre root item or null if tree has no input
     *
     * @return the root
     */
    TreeItem getRoot();

    /**
     * Find the item at the given path.
     * Only loaded items are searched.
     * This operation will not load any extra item.
     *
     * @param path the path to search
     * @return the item at the given path or null if none
     */
    TreeItem find(String path);

    /**
     * Find and item given it's path and expand parents if needed.
     * The returned item is not explicitly expanded so it may be collapsed or
     * its children if any not yet loaded
     *
     * @param path the path to search
     * @return the item or null if none
     */
    TreeItem findAndReveal(String path);

    /**
     * Like {@link #find(Path)} but the path is expressed as a {@link Path} object
     * @param path
     * @return
     * @see #find(String)
     */
    TreeItem find(Path path);

    /**
     * Like {@link #findAndReveal(Path)} but the path is expressed as a {@link Path} object
     * @param path
     * @return
     * @see #findAndReveal(String)
     */
    TreeItem findAndReveal(Path path);

}
