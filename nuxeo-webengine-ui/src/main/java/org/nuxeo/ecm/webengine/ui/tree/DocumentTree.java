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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DocumentTree {

    protected CoreSession session;
    protected TreeItem root;

    public DocumentTree(CoreSession session) {
        this.session = session;
    }

    public void setInput(String rootPath) throws ClientException {
        this.root = new TreeItemImpl(new DocumentContentProvider(session), session.getDocument(new PathRef(rootPath)));
    }

    public void setInput(DocumentRef root) throws ClientException {
        this.root = new TreeItemImpl(new DocumentContentProvider(session), session.getDocument(root));
    }

    public void setInput(DocumentModel root) {
        this.root = new TreeItemImpl(new DocumentContentProvider(session), root);
    }

    public DocumentModel getInput() {
        return (DocumentModel)this.root.getObject();
    }

    /**
     * @return the root.
     */
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
        Path rootPath = ((DocumentModel)root.getObject()).getPath();
        int p = path.matchingFirstSegments(rootPath);
        if (p == rootPath.segmentCount()) {
            return root.findAndReveal(path.removeFirstSegments(p));
        }
        return null;
    }

    public TreeItem find(Path path) {
        Path rootPath = ((DocumentModel)root.getObject()).getPath();
        int p = path.matchingFirstSegments(rootPath);
        if (p == rootPath.segmentCount()) {
            return root.find(path.removeFirstSegments(p));
        }
        return null;
    }

}
