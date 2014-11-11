/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 * (C) Copyright 2006-2007 YSEngineers Soc. Coop. And. (http://www.yerbabuena.es/) and contributors.

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
 *     Yerbabuena - initial implementation
 *     Nuxeo

 */

package org.nuxeo.ecm.platform.preview.seam;

import java.io.Serializable;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.preview.helper.PreviewHelper;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;

/**
 * Seam Action bean to handle the preview tabs and associated actions
 *
 * @author <a href="mailto:enriqueperez@yerbabuena.es">Enrique Pérez</a>
 * @author tiry
 */
@Name("previewActions")
@Scope(ScopeType.STATELESS)
public class PreviewActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    transient NavigationContext navigationContext;

    public boolean getHasPreview() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return documentHasPreview(currentDocument);
    }

    public boolean documentHasPreview(DocumentModel document) {
        if (document == null) {
            return false;
        }
        return PreviewHelper.typeSupportsPreview(document);
    }
    
    public String getPreviewURL() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument == null) {
            return null;
        }
        return PreviewHelper.getPreviewURL(currentDocument);
    }
    public String getPreviewWithBlobPostProcessingURL() {
        String url = getPreviewURL();
        url += "?blobPostProcessing=true";
        return url;
    }

}
