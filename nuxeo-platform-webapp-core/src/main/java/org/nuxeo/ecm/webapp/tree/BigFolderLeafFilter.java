/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.webapp.tree;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Filter;

/**
 * Simple filter that accepts if the document has the {@code BigFolder} facet.
 *
 * @author Florent Guillaume
 */
public class BigFolderLeafFilter implements Filter {

    private static final long serialVersionUID = 1L;

    public static final String BIG_FOLDER_FACET = "BigFolder";

    /**
     * Accepts if the document has the {@code BigFolder} facet.
     */
    public boolean accept(DocumentModel document) {
        return document.hasFacet(BIG_FOLDER_FACET);
    }

}
