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
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface TreeItem extends Serializable {

    int NONE = 0;

    int DATA = 1;

    int CHILDREN = 2;

    int BOTH = 3;

    /**
     * Gets the item path.
     * <p>
     * The path is uniquely identifying the item in its tree and is consistent with the tree structure so the parent
     * item will have the same path as the child minus the last segment. The root item path will always be "/". (The
     * root item should not be displayed in the tree - it has no label or other properties.)
     * <p>
     * Paths are useful to locate items in the tree using {@code find} methods.
     *
     * @return the item path
     * @see #find(Path)
     * @see #findAndReveal(Path)
     * @see TreeModel#find(Path)
     * @see TreeModel#findAndReveal(Path)
     */
    Path getPath();

    /**
     * Gets the object attached to this item.
     * <p>
     * The nature of the object depends on the registered content provider which will populate the tree branches when
     * {@link ContentProvider#getChildren(Object)} is called. The root item is specified by using
     * {@link TreeModel#setInput(Object)}
     *
     * @return the attached object or null if none
     */
    Object getObject();

    /**
     * Gets the parent item or null if this is the root item.
     *
     * @return the parent item
     */
    TreeItem getParent();

    /**
     * Gets this node name.
     * <p>
     * This is the same as the last segment on the item path
     */
    String getName();

    /**
     * Gets the label to be displayed for this item.
     */
    String getLabel();

    /**
     * Tests whether or not the item is expanded.
     *
     * @return true of expanded, false otherwise
     */
    boolean isExpanded();

    /**
     * Tests whether or not the item may have children.
     *
     * @return true if a container, false otherwise
     */
    boolean isContainer();

    /**
     * Gets the cached children.
     * <p>
     * The children items are created using the content provider the first time you call {@link #expand()}
     */
    TreeItem[] getChildren();

    /**
     * Gets the child item given its name.
     * <p>
     * This method will force loading children using the provider if not already loaded or if invalidated.
     *
     * @param name the name of the child item
     * @return the child item or null if none
     */
    TreeItem getChild(String name);

    /**
     * Tests whether this item has any children.
     * <p>
     * This method will not load children if not already loaded.
     *
     * @return true if the children item has children, false otherwise
     */
    boolean hasChildren();

    /**
     * Finds the item given its relative path to that item.
     * <p>
     * This method will search only the loaded items - it will not make additional calls to provider to get new items.
     *
     * @param path the item path to find
     * @return the item or null if none.
     */
    TreeItem find(Path path);

    /**
     * Finds the item given its relative path to that item and expand all its parents so that the item will be visible
     * in the tree.
     * <p>
     * The item itself will not be expanded. Use {@link #expand()} on the returned item if you want so.
     * <p>
     * This method is loading any parent if not already loaded by using the registered provider.
     *
     * @param path the item path to find
     * @return the item or null if none
     */
    TreeItem findAndReveal(Path path);

    /**
     * Expands the item.
     * <p>
     * This will load children items from the provider if they are not already loaded or if invalidated.
     */
    TreeItem[] expand();

    /**
     * Collapses this item. This will hide any loaded children.
     */
    void collapse();

    /**
     * Reloads item information like label, properties and children depending on the specified refresh type.
     * <p>
     * The argument is used to specify the type of refresh and can have one of the following values:
     * <ul>
     * <li>{@link #DATA} - to refresh only item data like labels
     * <li>{@link #CHILDREN} - to refresh only item children
     * <li>{@link #BOTH} - to refresh both data and children
     * </ul>
     *
     * @param type of refresh
     */
    void refresh(int type);

    /**
     * Invalidates the item.
     * <p>
     * This will force reloading item data and/or children next time item and/or children are accessed. The argument is
     * used to specify the type of invalidation and can have one of the following values:
     * <ul>
     * <li>{@link #DATA} - to invalidate only item data like labels
     * <li>{@link #CHILDREN} - to invalidate only item children
     * <li>{@link #BOTH} - to invalidate both data and children
     * </ul>
     *
     * @param type of invalidation
     */
    void invalidate(int type);

    /**
     * Validates the item.
     * <p>
     * If the item was not invalidated do nothing.
     */
    void validate();

    /**
     * Returns the validation state.
     * <p>
     * Can be one of:
     * <ul>
     * <li>{@link #DATA} - the item data is invalid (not loaded or invalidated)
     * <li>{@link #CHILDREN} - the item children are invalid
     * <li>{@link #BOTH} - both data and children are invalid
     * </ul>
     *
     * @return the validation state.
     */
    int getValidationState();

    /**
     * Gets the current content provider.
     *
     * @return the content provider. never return null
     */
    ContentProvider getContentProvider();

    /**
     * Accepts a visitor. This is to support visitor pattern.
     *
     * @param visitor the visitor to accept
     * @return the result of the visit
     */
    Object accept(TreeItemVisitor visitor);

}
