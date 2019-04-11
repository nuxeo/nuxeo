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

import org.nuxeo.common.utils.Path;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
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

    @Override
    public ContentProvider getContentProvider() {
        return provider;
    }

    @Override
    public void setContentProvider(ContentProvider provider) {
        this.provider = provider;
    }

    @Override
    public void setInput(Object input) {
        if (input == null) {
            root = null;
        } else {
            root = new TreeItemImpl(provider, input);
        }
    }

    @Override
    public TreeItem getRoot() {
        return root;
    }

    @Override
    public TreeItem findAndReveal(String path) {
        return findAndReveal(new Path(path));
    }

    @Override
    public TreeItem find(String path) {
        return find(new Path(path));
    }

    @Override
    public TreeItem findAndReveal(Path path) {
        if (root == null) {
            return null;
        }
        return root.findAndReveal(path);
    }

    @Override
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

    @Override
    public Object getInput() {
        return root == null ? null : root.getObject();
    }

    public boolean hasInput() {
        return root != null;
    }

}
