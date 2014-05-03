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

import java.io.Serializable;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.quota.QuotaStats;

/**
 * Tree node of documents.
 *
 * @author Anahide Tchertchian
 */
public interface DocumentTreeNode extends Serializable {

    List<DocumentTreeNode> getChildren();

    String getId();

    String getPath();

    /**
     * Returns the {@link QuotaStats} adapter for the underlying document of
     * this {@code DocumentTreeNode}.
     *
     * @since 5.5
     */
    QuotaStats getQuotaStats();

    /**
     * Returns true if node represents current document, or if it's the direct
     * parent of a non-folderish document that is not be represented in the
     * tree.
     *
     * @since 5.7
     * @param currentDocument
     */
    boolean isSelected(DocumentModel currentDocument);

    /**
     * Returns the document corresponding to this node
     */
    DocumentModel getDocument();

    void resetChildren();

    void fetchChildren();

    /**
     * @since 5.9.4
     */
    void setExpanded(boolean expanded);

    /**
     * @since 5.9.4
     */
    boolean isExpanded();

}
