/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.thumb;

import java.io.Serializable;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailAdapter;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.util.RepositoryLocation;

/**
 * Thumbnail Manager seam bean
 *
 * @since 5.7
 */
@Name("thumbnailManager")
@Scope(ScopeType.CONVERSATION)
public class ThumbnailManagerBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(required = true, create = true)
    protected transient NavigationContext navigationContext;

    /**
     * @since 5.7
     */
    public void downloadThumbnail(DocumentView docView) {
        if (docView != null) {
            DocumentLocation docLoc = docView.getDocumentLocation();
            if (documentManager == null) {
                RepositoryLocation loc = new RepositoryLocation(docLoc.getServerName());
                navigationContext.setCurrentServerLocation(loc);
                documentManager = navigationContext.getOrCreateDocumentManager();
            }
            DocumentModel doc = documentManager.getDocument(docLoc.getDocRef());
            if (doc != null) {
                ThumbnailAdapter thumbnailDoc = doc.getAdapter(ThumbnailAdapter.class);
                Blob thumbnail = thumbnailDoc.getThumbnail(documentManager);
                if (thumbnail == null) {
                    return;
                }
                ComponentUtils.download(doc, null, thumbnail, thumbnail.getFilename(), "thumbnail");
            }
        }
    }
}
