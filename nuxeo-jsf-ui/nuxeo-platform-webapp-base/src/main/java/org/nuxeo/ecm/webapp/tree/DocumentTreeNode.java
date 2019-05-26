/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
     * Returns the {@link QuotaStats} adapter for the underlying document of this {@code DocumentTreeNode}.
     *
     * @since 5.5
     */
    QuotaStats getQuotaStats();

    /**
     * Returns true if node represents current document, or if it's the direct parent of a non-folderish document that
     * is not be represented in the tree.
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
     * @since 6.0
     */
    void setExpanded(boolean expanded);

    /**
     * @since 6.0
     */
    boolean isExpanded();

}
