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

import org.nuxeo.common.utils.Path;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TreeModelImpl implements TreeModel {

    private static final long serialVersionUID = 1L;

    protected ContentProvider provider;
    protected TreeItem root;


    public TreeModelImpl() {
    }

    public TreeModelImpl(ContentProvider provider) {
        this.provider = provider;
    }

    public ContentProvider getContentProvider() {
        return provider;
    }

    public void setContentProvider(ContentProvider provider) {
        this.provider = provider;
    }

    public void setInput(Object input) {
        if (input == null) {
            root = null;
        } else {
            root = new TreeItemImpl(provider, input);
        }
    }

    public TreeItem getRoot() {
        return root;
    }

    public TreeItem findAndReveal(String path) {
        return findAndReveal(new Path(path));
    }

    public TreeItem find(String path) {
        return find(new Path(path));
    }

    public TreeItem findAndReveal(Path path) {
        if (root == null) {
            return null;
        }
        return root.findAndReveal(path);
    }

    public TreeItem find(Path path) {
        if (root == null) {
            return null;
        }
        Path rootPath = root.getPath();
        int p = path.matchingFirstSegments(rootPath);
        if (p == rootPath.segmentCount()) {
            return root.find(path.removeFirstSegments(p));
        }
        return null;
    }

    public Object getInput() {
        return root == null ? null : root.getObject();
    }

    public boolean hasInput() {
        return root != null;
    }

}
