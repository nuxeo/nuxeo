/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
