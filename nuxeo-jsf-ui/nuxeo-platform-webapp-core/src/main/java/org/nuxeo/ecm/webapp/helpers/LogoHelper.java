/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.helpers;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.IOException;
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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PropertyException;
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
        if (navigationContext == null || navigationContext.getCurrentServerLocation() == null) {
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
        } catch (DocumentNotFoundException e) {
            log.error(e, e);
            return DEFAULT_LOGO;
        }
    }

    public String getLogoURL(DocumentModel doc) {
        if (doc == null) {
            return DEFAULT_LOGO;
        }

        String key = doc.getCacheKey();
        if (key.equals(lastLogoHolderKey)) {
            return lastURL;
        }

        Blob blob = getBlob(doc);
        if (blob == null) {
            return DEFAULT_LOGO;
        }
        lastURL = PAGE_NAME + "?key=" + key + "&docRef=" + doc.getRef().toString() + '&'
                + getConversationPropagationSuffix();
        lastLogoHolderKey = doc.getCacheKey();
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
            } catch (PropertyException e) {
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
                } catch (DocumentNotFoundException e) {
                    log.error(e, e);
                }
            }
            imgBlob = getBlob(ob);
        }

        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        try {
            if (imgBlob == null
                    || (imgBlob.getMimeType() != null && !imgBlob.getMimeType().toLowerCase().startsWith("image"))) {
                response.setContentType("image/gif");
                response.sendRedirect(context.getExternalContext().getRequestContextPath() + DEFAULT_LOGO);
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

        } catch (IOException e) {
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
            suffix += conversationManager.getConversationIdParameter() + '=' + cId;
            /**
             * if (conversationManager.isLongRunningConversation()) { suffix += '&' +
             * conversationManager.getConversationIsLongRunningParameter() + "true"; lastMainConversation = cId; }
             **/
        } else {
            ConversationEntry conv = conversationManager.getCurrentConversationEntry();
            String convId = conv.getConversationIdStack().get(0);
            convId = getLastOrMainConversationId(convId);
            suffix += conversationManager.getConversationIdParameter() + '=' + convId;
            /**
             * suffix += '&' + conversationManager.getConversationIsLongRunningParameter() + "true";
             * lastMainConversation = convId;
             **/
        }

        return suffix;
    }

}
