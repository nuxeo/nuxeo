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

import javax.faces.context.FacesContext;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class ArchivedVersionsPathElement implements PathElement {

    public static final String TYPE = "ArchivedVersionsPathElement";

    private static final long serialVersionUID = 8965065837815754773L;

    private final DocumentModel docModel;

    public ArchivedVersionsPathElement(DocumentModel docModel) {
        this.docModel = docModel;
    }

    public String getName() {
        FacesContext context = FacesContext.getCurrentInstance();
        return ComponentUtils.translate(context, "label.archivedVersions");
    }

    public String getType() {
        return TYPE;
    }

    public boolean isLink() {
        return false;
    }

    public DocumentModel getDocumentModel() {
        return docModel;
    }

}
