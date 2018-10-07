/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     vdutat
 */
package org.nuxeo.ecm.platform.annotations.jsf.component;

import static org.jboss.seam.ScopeType.STATELESS;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsService;
import org.nuxeo.ecm.platform.preview.adapter.base.ConverterBasedHtmlPreviewAdapter;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Handles Annotations related web actions.
 *
 * @author <a href="mailto:vdutat@nuxeo.com">Vincent Dutat</a>
 * @since 5.7
 */
@Name("annotationsActions")
@Scope(STATELESS)
@Install(precedence = FRAMEWORK)
public class AnnotationsActions implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(AnnotationsActions.class);

    public static final String TEXT_ANNOTATIONS_KEY = "nuxeo.text.annotations";

    @In(create = true)
    protected transient NuxeoPrincipal currentUser;

    public long getAnnotationsCount(DocumentModel doc) {
        DocumentViewCodecManager documentViewCodecManager = Framework.getService(DocumentViewCodecManager.class);
        AnnotationsService annotationsService = Framework.getService(AnnotationsService.class);
        DocumentView docView = new DocumentViewImpl(doc);
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        String documentUrl = documentViewCodecManager.getUrlFromDocumentView("docpath", docView, true,
                VirtualHostHelper.getBaseURL(request));
        try {
            return annotationsService.getAnnotationsCount(new URI(documentUrl), currentUser);
        } catch (URISyntaxException e) {
            throw new NuxeoException(e);
        }
    }

    public boolean isAnnotationsEnabled(DocumentModel doc) {
        ConfigurationService cs = Framework.getService(ConfigurationService.class);
        if (cs.isBooleanPropertyFalse(ConverterBasedHtmlPreviewAdapter.OLD_PREVIEW_PROPERTY)) {
            // cannot work without old preview
            return false;
        }

        BlobHolder blobHolder = doc.getAdapter(BlobHolder.class);
        Blob blob = blobHolder.getBlob();
        if (blob == null || blob.getMimeType() == null) {
            return false;
        }

        return blob.getMimeType().startsWith("image") || isTextAnnotationsEnabled();
    }

    protected boolean isTextAnnotationsEnabled() {
        ConfigurationService cs = Framework.getService(ConfigurationService.class);
        return cs.isBooleanPropertyTrue(TEXT_ANNOTATIONS_KEY);
    }

}
