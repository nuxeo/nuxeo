/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import org.nuxeo.ecm.core.api.ClientException;
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
        pathTitles = new HashMap<String, String>();
    }

    /**
     * Adds a DocumentModel on a certain level.
     */
    public void add(DocumentModel document, int level) {
        DocumentModelTreeNode node = new DocumentModelTreeNodeImpl(document,
                level);
        add(node);
        String path = document.getPathAsString();
        String title;
        try {
            title = (String) document.getProperty("dublincore", "title");
        } catch (ClientException e) {
            title = null;
        }
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
