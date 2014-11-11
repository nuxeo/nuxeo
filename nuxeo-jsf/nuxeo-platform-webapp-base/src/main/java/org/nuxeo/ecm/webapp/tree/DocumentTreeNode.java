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

    // XXX add icon, url, label methods.

}
