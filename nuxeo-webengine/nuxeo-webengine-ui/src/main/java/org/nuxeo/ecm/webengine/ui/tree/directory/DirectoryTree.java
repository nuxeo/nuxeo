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

package org.nuxeo.ecm.webengine.ui.tree.directory;

import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.ui.tree.ContentProvider;
import org.nuxeo.ecm.webengine.ui.tree.JSonTree;
import org.nuxeo.ecm.webengine.ui.tree.JSonTreeSerializer;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class DirectoryTree extends JSonTree {

    protected Directory dir;


    protected DirectoryTree() {
    }

    protected DirectoryTree(Directory dir) {
        this.dir = dir;
    }

    protected DirectoryTree(String directory) throws Exception {
        this(Framework.getService(DirectoryService.class).getDirectory(directory));
    }

    @Override
    protected Object getInput(WebContext ctx) {
        return dir;
    }

    @Override
    protected ContentProvider getProvider(WebContext ctx) {
        try {
            return new DirectoryContentProvider(dir.getSession());
        } catch (DirectoryException e) {
            throw WebException.wrap(e);
        }
    }

    @Override
    protected JSonTreeSerializer getSerializer(WebContext ctx) {
        return new JSonTreeSerializer();
    }

}
