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


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TreeViewImpl implements TreeView {

    protected ContentProvider provider;
    protected TreeItem root;


    public TreeViewImpl(ContentProvider provider) {
        this.provider = provider;
    }

    public ContentProvider getContentProvider() {
        return provider;
    }

    public void setIntput(Object input) {
        this.root = new TreeItemImpl(provider, input);
    }

    public TreeItem getRoot() {
        return root;
    }

    public TreeItem findItem(String path) {
        return null;
    }
}
