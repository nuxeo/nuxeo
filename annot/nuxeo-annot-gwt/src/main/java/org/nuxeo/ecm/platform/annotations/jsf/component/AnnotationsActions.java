/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
import java.security.Principal;
import java.util.List;

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
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.api.AnnotationException;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsService;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

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
    protected transient Principal currentUser;

    public long getAnnotationsCount(DocumentModel doc) {
        DocumentViewCodecManager documentViewCodecManager = Framework.getLocalService(DocumentViewCodecManager.class);
        AnnotationsService annotationsService = Framework.getLocalService(AnnotationsService.class);
        DocumentView docView = new DocumentViewImpl(doc);
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        String documentUrl = documentViewCodecManager.getUrlFromDocumentView(
                "docpath", docView, true, VirtualHostHelper.getBaseURL(request));
        try {
            List<Annotation> annotations = annotationsService.queryAnnotations(
                    new URI(documentUrl), null, (NuxeoPrincipal) currentUser);
            return annotations.size();
        } catch (AnnotationException e) {
            log.error("Unable to get annotations graph", e);
            return 0;
        } catch (URISyntaxException e) {
            log.error("Unable to get annotations for: " + documentUrl, e);
            return 0;
        }
    }

    public boolean isAnnotationsEnabled(DocumentModel doc) {
        BlobHolder blobHolder = doc.getAdapter(BlobHolder.class);
        Blob blob = blobHolder.getBlob();
        if (blob == null || blob.getMimeType() == null) {
            return false;
        }

        return Framework.isBooleanPropertyTrue(TEXT_ANNOTATIONS_KEY)
                || blob.getMimeType().startsWith("image");
    }

}
