/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.helpers;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.ConversationEntry;
import org.jboss.seam.core.Manager;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;

@Name("logoHelper")
@Scope(CONVERSATION)
public class LogoHelper implements Serializable {

    private static final long serialVersionUID = 876540986876L;

    private static final String PAGE_NAME = "/showLogo.faces";

    private static final String DEFAULT_LOGO = "/img/default_logo.gif";

    private static final Log log = LogFactory.getLog(LogoHelper.class);

    @In(value = "org.jboss.seam.core.manager")
    public transient Manager conversationManager;

    @In(create = true, required = false)
    transient NavigationContext navigationContext;

    @In(create = true, required = false)
    transient CoreSession documentManager;

    private String lastLogoHolderKey = "";

    private DocumentModel lastLogoHolder;

    private String lastURL = "";

    private String lastMainConversation = "";

    public String getLogoURL() {
        if (navigationContext == null
                || navigationContext.getCurrentServerLocation() == null) {
            lastLogoHolderKey = "";
            lastURL = "";
            return DEFAULT_LOGO;
        }

        DocumentModel ws = navigationContext.getCurrentWorkspace();

        return getLogoURL(ws);
    }

    public String getDefaultLogoURL() {
        return DEFAULT_LOGO;
    }

    public String getLogoURLFromDocRef(String docRef) {
        if (documentManager == null) {
            return DEFAULT_LOGO;
        }

        DocumentRef ref = new IdRef(docRef);

        try {
            DocumentModel doc = documentManager.getDocument(ref);
            return getLogoURL(doc);

        } catch (ClientException e) {
            log.error(e);
            return DEFAULT_LOGO;
        }
    }

    public String getLogoURL(DocumentModel doc) {
        if (doc == null) {
            return DEFAULT_LOGO;
        }

        String key;
        try {
            key = doc.getCacheKey();
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }

        if (key.equals(lastLogoHolderKey)) {
            return lastURL;
        }

        Blob blob = getBlob(doc);
        if (blob == null) {
            return DEFAULT_LOGO;
        }
        lastURL = PAGE_NAME + "?key=" + key + "&docRef="
                + doc.getRef().toString() + '&'
                + getConversationPropagationSuffix();
        try {
            lastLogoHolderKey = doc.getCacheKey();
        } catch (ClientException e) {
            lastLogoHolderKey = null;
        }
        lastLogoHolder = doc;

        return lastURL;
    }

    private static Blob getBlob(DocumentModel doc) {
        if (doc == null) {
            return null;
        }
        if (doc.hasSchema("file")) {
            try {
                return (Blob) doc.getProperty("file", "content");
            } catch (ClientException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    @RequestParameter
    String key;

    @RequestParameter
    String docRef;

    public String getLogo() {
        Blob imgBlob = null;

        // returns cached blob
        if (key != null && key.equals(lastLogoHolderKey)) {
            imgBlob = getBlob(lastLogoHolder);
        }

        // recompute blob
        if (imgBlob == null) {
            DocumentModel ob = navigationContext.getCurrentWorkspace();
            if (ob == null || !ob.getRef().toString().equals(docRef)) {
                DocumentRef ref = new IdRef(docRef);
                try {
                    ob = documentManager.getDocument(ref);
                } catch (ClientException e) {
                    // TODO: more robust exception handling?
                    log.error(e);
                }
            }
            imgBlob = getBlob(ob);
        }

        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        try {
            if (imgBlob == null
                    || (imgBlob.getMimeType() != null && !imgBlob.getMimeType().toLowerCase().startsWith(
                            "image"))) {
                response.setContentType("image/gif");
                response.sendRedirect(context.getExternalContext().getRequestContextPath()
                        + DEFAULT_LOGO);
                return null;
            } else {
                response.addHeader("Cache-Control", "max-age=600");
                response.addHeader("Cache-Control", "public");
                response.setContentType(imgBlob.getMimeType());
                response.getOutputStream().write(imgBlob.getByteArray());
                response.getOutputStream().close();
                response.flushBuffer();
                context.responseComplete();
            }

        } catch (Exception e) {
            log.error("error while sending logo: ", e);
        }

        return null;
    }

    private String getLastOrMainConversationId(String cId) {
        if (!cId.startsWith("0NX")) {
            cId = lastMainConversation;
        }

        if (lastMainConversation == null || lastMainConversation.equals("")) {
            cId = "0NXMAIN";
        }
        return cId;
    }

    private String getConversationPropagationSuffix() {
        String suffix = "";

        if (!conversationManager.getCurrentConversationEntry().isNested()) {
            String cId = conversationManager.getCurrentConversationId();
            // tmp hack to handle the case when the logo is rendered
            // just after the page existed the conversation
            cId = getLastOrMainConversationId(cId);
            suffix += conversationManager.getConversationIdParameter() + '='
                    + cId;
            /**
             * if (conversationManager.isLongRunningConversation()) { suffix +=
             * '&' + conversationManager.getConversationIsLongRunningParameter()
             * + "true"; lastMainConversation = cId; }
             **/
        } else {
            ConversationEntry conv = conversationManager.getCurrentConversationEntry();
            String convId = conv.getConversationIdStack().get(0);
            convId = getLastOrMainConversationId(convId);
            suffix += conversationManager.getConversationIdParameter() + '='
                    + convId;
            /**
             * suffix += '&' +
             * conversationManager.getConversationIsLongRunningParameter() +
             * "true"; lastMainConversation = convId;
             **/
        }

        return suffix;
    }

}
