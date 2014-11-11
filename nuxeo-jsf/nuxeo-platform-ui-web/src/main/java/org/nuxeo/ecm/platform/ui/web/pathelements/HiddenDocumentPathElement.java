/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.pathelements;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.FacetNames;

/**
 * Represents a document path element, with no visible link.
 * <p>
 * Useful when representing documents in the breadcrumbs that are marked as
 * {@link FacetNames#HIDDEN_IN_NAVIGATION}.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class HiddenDocumentPathElement extends DocumentPathElement {

    private static final long serialVersionUID = 1L;

    public HiddenDocumentPathElement(DocumentModel docModel) {
        super(docModel);
    }

    @Override
    public boolean isLink() {
        return false;
    }

}
