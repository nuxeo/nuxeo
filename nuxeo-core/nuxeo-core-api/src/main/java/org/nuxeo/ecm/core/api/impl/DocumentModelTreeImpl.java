/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelTree;
import org.nuxeo.ecm.core.api.DocumentModelTreeNode;

/**
 * @author <a href="mailto:npaslaru@nuxeo.com">Paslaru Narcis</a>
 *
 */
public class DocumentModelTreeImpl extends ArrayList<DocumentModelTreeNode>
        implements DocumentModelTree {

    private static final long serialVersionUID = -6980985131163070762L;

    protected final Map<String, String> pathTitles;

    public DocumentModelTreeImpl() {
        super();
        pathTitles = new HashMap<String, String>();
    }

    /**
     * Adds a DocumentModel on a certain level.
     *
     */
    public void add(DocumentModel document, int level) {
        DocumentModelTreeNode node = new DocumentModelTreeNodeImpl(document,
                level);
        add(node);
        String path = document.getPathAsString();
        String title = (String) document.getProperty("dublincore", "title");
        title = (title != null) ? title.toLowerCase() : title;
        pathTitles.put(path, title);
    }

    /**
     * Get a mapping used by comparator like DocumentModelTreeNodeComparator
     *
     * @return a map path/title
     */
    public Map<String, String> getPathTitles() {
        return pathTitles;
    }
}
