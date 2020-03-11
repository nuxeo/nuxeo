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

package org.nuxeo.ecm.webengine.ui.tree.directory;

import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.ui.tree.ContentProvider;
import org.nuxeo.ecm.webengine.ui.tree.JSonTree;
import org.nuxeo.ecm.webengine.ui.tree.JSonTreeSerializer;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class DirectoryTree extends JSonTree {

    protected Directory dir;

    protected DirectoryTree() {
    }

    protected DirectoryTree(Directory dir) {
        this.dir = dir;
    }

    protected DirectoryTree(String directory) {
        this(Framework.getService(DirectoryService.class).getDirectory(directory));
    }

    @Override
    protected Object getInput(WebContext ctx) {
        return dir;
    }

    @Override
    protected ContentProvider getProvider(WebContext ctx) {
        return new DirectoryContentProvider(dir.getSession());
    }

    @Override
    protected JSonTreeSerializer getSerializer(WebContext ctx) {
        return new JSonTreeSerializer();
    }

}
