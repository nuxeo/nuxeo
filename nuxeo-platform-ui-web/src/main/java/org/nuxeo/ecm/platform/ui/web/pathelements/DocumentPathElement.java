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

package org.nuxeo.ecm.platform.ui.web.pathelements;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class DocumentPathElement implements PathElement {

    public static final String TYPE = "DocumentPathElement";

    private static final long serialVersionUID = 3539843847014749832L;

    protected final DocumentModel docModel;


    public DocumentPathElement(DocumentModel docModel) {
        this.docModel = docModel;
    }

    public String getName() {
        return DocumentModelFunctions.titleOrId(docModel);
    }

    public DocumentModel getDocumentModel() {
        return docModel;
    }

    public String getType() {
        return TYPE;
    }

    public boolean isLink() {
        return true;
    }

}
