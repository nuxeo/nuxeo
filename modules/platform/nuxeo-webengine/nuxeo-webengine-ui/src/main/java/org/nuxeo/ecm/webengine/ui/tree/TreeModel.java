/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.ui.tree;

import java.io.Serializable;

import org.nuxeo.common.utils.Path;

/**
 * A tree view manage a tree structure of items. The tree data is lazy loaded by using the data provider specified at
 * tree view creation.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface TreeModel extends Serializable {

    /**
     * Gets the content provider used by this tree.
     */
    ContentProvider getContentProvider();

    /**
     * Sets the content provider to be used by this tree.
     */
    void setContentProvider(ContentProvider provider);

    /**
     * Sets the input data.
     *
     * @param input (may be null)
     */
    void setInput(Object input);

    /**
     * Gets the current input of the tree.
     *
     * @return the tree input data. may be null.
     */
    Object getInput();

    /**
     * Get the tree root item, or null if tree has no input.
     *
     * @return the root
     */
    TreeItem getRoot();

    /**
     * Find the item at the given path. Only loaded items are searched. This operation will not load any extra item.
     *
     * @param path the path to search
     * @return the item at the given path or null if none
     */
    TreeItem find(String path);

    /**
     * Find and item given it's path and expand parents if needed. The returned item is not explicitly expanded so it
     * may be collapsed or its children if any not yet loaded
     *
     * @param path the path to search
     * @return the item or null if none
     */
    TreeItem findAndReveal(String path);

    /**
     * Like {@link #find(Path)} but the path is expressed as a {@link Path} object.
     *
     * @see #find(String)
     */
    TreeItem find(Path path);

    /**
     * Like {@link #findAndReveal(Path)} but the path is expressed as a {@link Path} object.
     *
     * @see #findAndReveal(String)
     */
    TreeItem findAndReveal(Path path);

}
