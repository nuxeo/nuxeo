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
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface TreeItem extends Serializable {

    public static final int NONE = 0;
    public static final int DATA = 1;
    public static final int CHILDREN = 2;
    public static final int BOTH = 3;

    /**
     * Get the item path. The path is uniquely identifying the item in its tree
     *  and is consistent with the tree structure so the parent item will have the same path
     *  as the child minus the last segment. The root item path will always be "/".
     *  (The root item should not be displayed in the tree - it has no label or other properties.
     *  <p>
     *  Paths are useful to locate items in the tree using <code>find</find> methods
     *
     * @return the item path
     *
     * @see #find(Path)
     * @see #findAndReveal(Path)
     * @see TreeView#find(Path)
     * @see TreeView#findAndReveal(Path)
     */
    Path getPath();

    /**
     * Get the object attached to this item.
     * The nature of the object depends on the registered content provider
     * which will populate the tree branches when {@link ContentProvider#getChildren(Object)} is called.
     * The root item is specified by using {@link TreeView#setInput(Object)}
     *
     * @return the attached object or null if none
     */
    Object getObject();

    /**
     * Get the parent item or null if this is the root item
     * @return the parent item
     */
    TreeItem getParent();

    /**
     * Get this node name. This is the same as the last segment on the item path
     */
    String getName();

    /**
     * The label to be displayed for this item
     * @return
     */
    String getLabel();

    /**
     * Test whether or not the item is expanded
     * @return true of expanded, false otherwise
     */
    boolean isExpanded();

    /**
     * Test whether or not the item may have children
     * @return true if a container, false otherwise
     */
    boolean isContainer();

    /**
     * Get the cached children.
     * The children items are created using the content provider
     * the first time you call {@link #expand()}
     * @return
     */
    TreeItem[] getChildren();

    /**
     * Get the child item given its name.
     * This method will force loading children using the provider if not already
     * loaded or if invalidated
     * @param name the name of the child item
     * @return the child item or null if none
     */
    TreeItem getChild(String name);

    /**
     * Tests whether this item has any children.
     * This method will not load children if not already loaded.
     *
     * @return true if the children item has children, false otherwise
     */
    boolean hasChildren();

    /**
     * Find the item given its relative path to that item
     * <p>
     * This method will search only the loaded items - it
     * will not make additional calls to provider to get new items.
     * @param path the item path to find
     * @return the item or null if none.
     */
    TreeItem find(Path path);

    /**
     * Find the item given its relative path to that item and expand all its parents
     * so that the item will be visible in the tree.
     * The item itself will not be expanded. Use {@link #expand()} on the returned item
     * if you want so.
     * <p>
     * This method is loading any parent if not already loaded by using the registered provider.
     *
     * @param path the item path to find
     * @return the item or null if none
     */
    TreeItem findAndReveal(Path path);

    /**
     * Expand the item. This will load children items from the provider
     * if they are not already loaded or if invalidated
     *
     * @return
     */
    TreeItem[] expand();

    /**
     * Collapse this item. This will hide any loaded children.
     */
    void collapse();

    /**
     * Reload item information like label, properties and children depending
     * on the specified refresh type.
     * The argument is used to specify the type of refresh and can have
     * one of the following values:
     *      {@link #DATA} - to refresh only item data like labels
     *      {@link #CHILDREN} - to refresh only item children
     *      {@link #BOTH} - to refresh both data and children
     *  @param type of refresh
     */
    void refresh(int type);

    /**
     * Invalidate the item. This will force reloading item data and/or children
     * next time item and/or children are accessed.
     * The argument is used to specify the type of invalidation and can have
     * one of the following values:
     *      {@link #DATA} - to invalidate only item data like labels
     *      {@link #CHILDREN} - to invalidate only item children
     *      {@link #BOTH} - to invalidate both data and children
     * @param type of invalidation
     */
    void invalidate(int type);

    /**
     * Validate the item.
     * If the item was not invalidated do nothing.
     */
    void validate();

    /**
     * Return the validation state. Can be one of
     *      {@link #DATA} - the item data is invalid (not loaded or invalidated)
     *      {@link #CHILDREN} - the item children are invalid
     *      {@link #BOTH} - both data and children are invalid
     * @return the validation state.
     */
    int getValidationState();

    /**
     * Get the current content provider
     * @return the content provider. never return null
     */
    ContentProvider getContentProvider();

    /**
     * Accept a visitor. This is to support visitor pattern.
     *
     * @param visitor the visitor to accept
     * @return the result of the visit
     */
    Object accept(TreeItemVisitor visitor);

}
