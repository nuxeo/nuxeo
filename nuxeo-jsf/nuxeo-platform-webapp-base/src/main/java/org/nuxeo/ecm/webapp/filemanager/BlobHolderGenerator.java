/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.webapp.filemanager;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;

@Name("blobHolderGenerator")
@Scope(ScopeType.EVENT)
public class BlobHolderGenerator {

    @In(create = true, required = false)
    private NavigationContext navigationContext;

    @Factory(value = "currentDocumentAsBlobHolder", scope = ScopeType.EVENT)
    public BlobHolder getCurrentBlobHolder() {
        if (navigationContext == null) {
            return null;
        }
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return getBlobHolder(currentDocument);
    }

    public BlobHolder getBlobHolder(DocumentModel document) {
        if (document != null) {
            return document.getAdapter(BlobHolder.class);
        } else {
            return null;
        }
    }
}
