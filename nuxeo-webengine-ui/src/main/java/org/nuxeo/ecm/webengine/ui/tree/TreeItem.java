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

    Object getObject();

    TreeItem getParent();

    String getName();

    boolean isExpanded();

    boolean hasChildren();

    /**
     * Get the cached children. Use expand to load children from provider
     * @return
     */
    TreeItem[] getChildren();


    TreeItem getChild(String name);

    boolean isLoaded();

    public TreeItem find(Path path);

    public TreeItem findAndReveal(Path path);

    public TreeItem[] expand();

    public void collapse();

    public void refresh();

    ContentProvider getContentProvider();

    Object accept(TreeItemVisitor visitor);

}
