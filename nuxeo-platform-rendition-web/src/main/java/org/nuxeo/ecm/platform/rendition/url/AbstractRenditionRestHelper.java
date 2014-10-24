/*
 * (C) Copyright 2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.rendition.url;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.In;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendition.RenditionException;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.util.RepositoryLocation;

/**
 * Base class for Rendition url codec bindings.
 * <p>
 * This class is shared with Template rendering system.
 *
 * @since 5.6
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public abstract class AbstractRenditionRestHelper implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static final Log log = LogFactory.getLog(AbstractRenditionRestHelper.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    protected abstract Blob renderAsBlob(DocumentModel doc, String renditionName)
            throws Exception;

    @Begin(id = "#{conversationIdGenerator.nextMainConversationId}", join = true)
    public void render(DocumentView docView) throws Exception {

        DocumentLocation docLoc = docView.getDocumentLocation();
        if (documentManager == null) {
            RepositoryLocation loc = new RepositoryLocation(
                    docLoc.getServerName());
            navigationContext.setCurrentServerLocation(loc);
            documentManager = navigationContext.getOrCreateDocumentManager();
        }
        DocumentModel doc = documentManager.getDocument(docLoc.getDocRef());
        if (doc != null) {
            String renditionName = docView.getParameter(RenditionBasedCodec.RENDITION_PARAM_NAME);
            FacesContext context = FacesContext.getCurrentInstance();
            Blob rendered = null;
            try {
                rendered = renderAsBlob(doc, renditionName);
            } catch (RenditionException e) {
                log.error("Unable to generate rendition " + renditionName, e);
                facesMessages.add(StatusMessage.Severity.WARN,
                        messages.get("rendition.not.available"), renditionName);
                // now we need to redirect
                // otherwise the page will be rendered via Seam PDF
                HttpServletRequest req = (HttpServletRequest) context.getExternalContext().getRequest();
                String url = DocumentModelFunctions.documentUrl(doc, req);

                HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
                try {
                    response.sendRedirect(url);
                    // be sure we block the codec chain
                    response.flushBuffer();
                    FacesContext.getCurrentInstance().responseComplete();
                } catch (IOException ioe) {
                    log.error("Error while redirecting to standard view", ioe);
                }
                return;
            }
            if (rendered != null) {
                if (rendered.getMimeType() != null
                        && rendered.getMimeType().startsWith("text/")) {
                    HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
                    // add inline download flag
                    request.setAttribute("inline", "true");
                }
                ComponentUtils.download(context, rendered,
                        rendered.getFilename());
            } else {
                HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
                response.sendError(404, "Unable to find rendition "
                        + renditionName);
            }
        }
    }

}