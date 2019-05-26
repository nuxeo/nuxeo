/*
 * (C) Copyright 2013-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.picture.recompute;

import static org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants.PICTURE_FACET;
import static org.nuxeo.ecm.platform.picture.recompute.RecomputeViewsAction.ACTION_NAME;
import static org.nuxeo.ecm.platform.picture.recompute.RecomputeViewsAction.PARAM_XPATH;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.contentbrowser.DocumentActions;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

@Name("imagingRecomputeActions")
@Scope(ScopeType.CONVERSATION)
public class ImagingRecomputeActions implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_NXQL_QUERY = "SELECT * FROM Document WHERE ecm:mixinType = 'Picture' AND picture:views/*/title IS NULL";

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient DocumentActions documentActions;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    protected String nxqlQuery = DEFAULT_NXQL_QUERY;

    public String getNxqlQuery() {
        return nxqlQuery;
    }

    public void setNxqlQuery(String nxqlQuery) {
        this.nxqlQuery = nxqlQuery;
    }

    public void recomputePictureViews() {
        recomputePictureViews(navigationContext.getCurrentDocument());
    }

    public void recomputePictureViews(DocumentModel doc) {
        if (doc.hasFacet(PICTURE_FACET)) {
            BlobHolder blobHolder = doc.getAdapter(BlobHolder.class);
            if (blobHolder.getBlob() != null) {
                blobHolder.setBlob(blobHolder.getBlob());
                Events.instance().raiseEvent(EventNames.BEFORE_DOCUMENT_CHANGED, doc);
                documentManager.saveDocument(doc);
                documentManager.save();
                navigationContext.invalidateCurrentDocument();
            }
            facesMessages.addFromResourceBundle(StatusMessage.Severity.INFO, "label.imaging.recompute.views.done");
        }
    }

    public void launchPictureViewsRecomputation() {
        if (!StringUtils.isBlank(nxqlQuery)) {
            BulkService service = Framework.getService(BulkService.class);
            String user = documentManager.getPrincipal().getName();
            service.submit(new BulkCommand.Builder(ACTION_NAME, nxqlQuery, user)
                    .param(PARAM_XPATH, "file:content")
                    .build());
            facesMessages.addFromResourceBundle(StatusMessage.Severity.INFO, "label.imaging.recompute.work.launched");
        }

    }
}
