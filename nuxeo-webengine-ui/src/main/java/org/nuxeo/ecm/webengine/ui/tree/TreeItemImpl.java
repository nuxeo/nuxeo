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

import java.util.Map;

import org.nuxeo.common.utils.Path;



/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TreeItemImpl implements TreeItem {

    private static final long serialVersionUID = 5252830785508229998L;

    public final static TreeItem[] EMPTY_CHILDREN = new TreeItem[0];
    public final static TreeItem[] HAS_CHILDREN = new TreeItem[0];

    protected TreeItem parent;
    protected ContentProvider provider;
    protected transient Object obj;
    protected TreeItem[] children = EMPTY_CHILDREN;
    protected boolean isExpanded = false;
    protected boolean isLoaded = false; // whether or not children are known
    protected String name;
    protected Map<String, TreeItem> childrenMap; //TODO: use a map?

    public TreeItemImpl(ContentProvider provider, Object data) {
        this (null, provider, data);
    }

    public TreeItemImpl(TreeItem parent, ContentProvider provider, Object data) {
        this.parent = parent;
        this.provider = provider;
        this.obj = data;
        this.name = provider.getName(obj);
    }

    public TreeItemImpl(TreeItem parent, Object data) {
        this (parent, parent.getContentProvider(), data);
    }

    public boolean isLoaded() {
        return children != HAS_CHILDREN && children != EMPTY_CHILDREN;
    }

    public TreeItem[] getChildren() {
        return children;
    }

    public Object getObject() {
        return obj;
    }

    public TreeItem getParent() {
        return parent;
    }

    public ContentProvider getContentProvider() {
        return provider;
    }

    public String getName() {
        return name;
    }

    public boolean hasChildren() {
        if (children == EMPTY_CHILDREN) {
            boolean hasChildren = provider.hasChildren(obj);
            if (!hasChildren) {
                children = HAS_CHILDREN;
            }
            return hasChildren;
        } else {
            return children != null;
        }
    }

    public TreeItem find(Path path) {
        TreeItem item = this;
        for (int i=0,len=path.segmentCount()-1; i<len; i++) {
            if (!item.isLoaded()) {
                return null;
            }
            item = item.getChild(path.segment(i));
            if (item == null) {
                return null;
            }
        }
        if (!item.isLoaded()) {
            return null;
        }
        return item.getChild(path.lastSegment());
    }

    public TreeItem findAndReveal(Path path) {
        // we expand only parents and not the last segment
        TreeItem item = this;
        int len = path.segmentCount();
        for (int i=0; i<len; i++) {
            item.expand();
            item = item.getChild(path.segment(i));
            if (item == null) {
                return null;
            }
        }
        return item;
    }

    public TreeItem getChild(String name) {
        loadChildren(); // force children loading
        return _getChild(name);
    }

    protected TreeItem _getChild(String name) {
        for (TreeItem child : children) {
            if (name.equals(child.getName())) {
                return child;
            }
        }
        return null;
    }

    public TreeItem[] expand() {
        if (isExpanded) {
            return children;
        } else {
            if (parent != null && !parent.isExpanded()) {
                parent.expand();
            }
            isExpanded = true;
            return loadChildren();
        }
    }

    public TreeItem[] loadChildren() {
        if (!isLoaded) {
            Object[] objects = provider.getChildren(obj);
            if (objects == null) {
                children = null;
            } else {
                children = new TreeItemImpl[objects.length];
                if (objects != null) {
                    for (int i=0; i<objects.length; i++) {
                        children[i] = new TreeItemImpl(this, objects[i]);
                    }
                }
            }
            isLoaded = true;
        }
        return children;
    }

    public void collapse() {
        isExpanded = false;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void refresh() {
        children = EMPTY_CHILDREN;
        if (isExpanded) {
            loadChildren();
        }
    }

    public Object accept(TreeItemVisitor visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "TreeItem: "+obj.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof TreeItem) {
            return obj.equals(((TreeItem)obj).getObject());
        }
        return false;
    }

}
