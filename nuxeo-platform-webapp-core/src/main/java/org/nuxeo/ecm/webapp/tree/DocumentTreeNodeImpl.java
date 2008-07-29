/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.webapp.tree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.Sorter;
import org.nuxeo.ecm.core.api.security.SecurityConstants;

/**
 * Tree node of documents.
 *
 * <p>
 * Children are lazy-loaded from backend only when needed.
 * </p>
 *
 * @author Anahide Tchertchian
 *
 */
public class DocumentTreeNodeImpl implements DocumentTreeNode {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DocumentTreeNodeImpl.class);

    protected final DocumentModel document;

    protected final String sessionId;

    protected final Filter filter;

    protected final Sorter sorter;

    protected Map<Object, DocumentTreeNodeImpl> children = null;

    public DocumentTreeNodeImpl(DocumentModel document, Filter filter,
            Sorter sorter) {
        super();
        this.document = document;
        this.sessionId = document.getSessionId();
        this.filter = filter;
        this.sorter = sorter;
    }

    public List<DocumentTreeNode> getChildren() {
        if (children == null) {
            fetchChildren();
        }
        List<DocumentTreeNode> childrenNodes = new ArrayList<DocumentTreeNode>();
        childrenNodes.addAll(children.values());
        return childrenNodes;
    }

    public DocumentModel getDocument() {
        return document;
    }

    public String getId() {
        if (document != null) {
            return document.getId();
        }
        return null;
    }

    public String getPath() {
        if (document != null) {
            return document.getPathAsString();
        }
        return null;
    }

    /**
     * Resets children map
     */
    public void resetChildren() {
        this.children = null;
    }

    public void fetchChildren() {
        try {
            children = new LinkedHashMap<Object, DocumentTreeNodeImpl>();
            CoreSession session = CoreInstance.getInstance().getSession(
                    sessionId);

            // get and filter
            DocumentModelList coreChildren = session.getChildren(
                    document.getRef(), null, SecurityConstants.READ, filter,
                    sorter);
            for (DocumentModel child : coreChildren) {
                String identifier = child.getId();
                DocumentTreeNodeImpl childNode = new DocumentTreeNodeImpl(
                        child, filter, sorter);
                children.put(identifier, childNode);
            }

        } catch (ClientException e) {
            log.error(e);
        }
    }

}