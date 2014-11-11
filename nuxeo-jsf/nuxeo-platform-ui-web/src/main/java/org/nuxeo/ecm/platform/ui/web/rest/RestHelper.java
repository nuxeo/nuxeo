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

package org.nuxeo.ecm.platform.ui.web.rest;

import static org.jboss.seam.ScopeType.EVENT;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.Manager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.util.RepositoryLocation;

/**
 * Helper for generation of URLs and related update of document context.
 *
 * @author tiry
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 * @author Florent Guillaume
 */
@Name("restHelper")
@Scope(EVENT)
public class RestHelper implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true)
    transient NavigationContext navigationContext;

    @In(create = true)
    transient WebActions webActions;

    private DocumentView docView;

    private String baseURL = "";

    /**
     * Sets current server location (core repository) and core document as
     * provided by the document view.
     * <p>
     * Only sets current server location if the document reference is null.
     */
    @Begin(id = "#{conversationIdGenerator.currentOrNewMainConversationId}", join = true)
    public String initContextFromRestRequest(DocumentView docView)
            throws ClientException {
        String outcome = null;

        if (docView != null) {
            DocumentLocation docLoc = docView.getDocumentLocation();
            String serverName = docLoc.getServerName();
            if (serverName != null) {
                DocumentRef docRef = docLoc.getDocRef();
                RepositoryLocation repoLoc = new RepositoryLocation(serverName);
                if (docRef != null) {
                    outcome = navigationContext.navigateTo(repoLoc, docRef);
                } else {
                    navigationContext.setCurrentServerLocation(repoLoc);
                }
            }
        }

        return outcome;
    }

    public void setDocumentView(DocumentView docView) {
        this.docView = docView;
    }

    public DocumentView getNewDocumentView() {
        DocumentView docView = null;
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument != null) {
            // XXX AT: i dont get why currentServerLocation is null while
            // currentDocument is not..
            DocumentLocation docLoc = new DocumentLocationImpl(currentDocument);
            TypeInfo typeInfo = currentDocument.getAdapter(TypeInfo.class);
            Map<String, String> params = new HashMap<String, String>();
            params.put("tabId", webActions.getCurrentTabId());
            params.put("subTabId", webActions.getCurrentSubTabId());
            if (currentDocument.isVersion()) {
                params.put("version", "true");
            }
            docView = new DocumentViewImpl(docLoc, typeInfo.getDefaultView(),
                    params);
        }
        return docView;
    }

    public DocumentView getDocumentView() {
        return docView;
    }

    /**
     * @return the Seam conversation manager.
     */
    public static Manager getConversationManager() {
        if (Contexts.isEventContextActive()) {
            return Manager.instance();
        }
        return null;
    }

    protected static String addConversationRequestParameters(String url,
            Manager conversationManager, String conversationId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(conversationManager.getConversationIdParameter(),
                conversationId);
        return conversationManager.encodeParameters(url, params);
    }

    /**
     * Adds current conversation request parameters to the given url.
     *
     * @param url
     * @return the url with additional conversation request parameters
     */
    public static String addCurrentConversationParameters(String url) {
        Manager conversationManager = getConversationManager();
        if (conversationManager == null) {
            return url;
        }
        // XXX : deprecated
        return conversationManager.encodeConversationId(url);
    }

    /**
     * Adds main conversation request parameters to the given url.
     *
     * @param url
     * @return the url with additional conversation request parameters
     */
    public static String addMainConversationParameters(String url) {
        Manager conversationManager = getConversationManager();
        if (conversationManager == null) {
            return url;
        } else {
            String conversationId;
            if (conversationManager.isNestedConversation()) {
                conversationId = conversationManager.getParentConversationId();
            } else {
                conversationId = conversationManager.getCurrentConversationId();
            }
            return addConversationRequestParameters(url, conversationManager,
                    conversationId);
        }
    }

    public String getDocumentUrl(DocumentModel doc) {
        return DocumentModelFunctions.documentUrl(doc);
    }

    public String getDocumentUrl(DocumentModel doc, String viewId,
            boolean newConversation) {
        return DocumentModelFunctions.documentUrl(null, doc, viewId, null,
                newConversation, null);
    }

    public String getDocumentUrl(String patternName, DocumentModel doc,
            String viewId, Map<String, String> parameters,
            boolean newConversation) {
        return DocumentModelFunctions.documentUrl(patternName, doc, viewId,
                parameters, newConversation, null);
    }

    @Factory(value = "baseURL", scope = ScopeType.CONVERSATION)
    public String getBaseURL() {
        if (baseURL.equals("")) {
            baseURL = BaseURL.getBaseURL();
        }
        return baseURL;
    }

    @Factory(value = "contextPath", scope = ScopeType.CONVERSATION)
    public String getContextPath() {
        return BaseURL.getContextPath();
    }

    public String doPrint(String defaultTheme) throws IOException {
        return doPrint(navigationContext.getCurrentDocument(), defaultTheme);
    }

    public String doPrint(DocumentModel doc, String defaultTheme)
            throws IOException {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            return null;
        }
        HttpServletResponse response = getHttpServletResponse();
        if (response != null) {
            handleRedirect(response, getPrintUrl(doc, defaultTheme));
        }
        return null;
    }

    public String getPrintUrl(String defaultTheme) {
        return getPrintUrl(navigationContext.getCurrentDocument(), defaultTheme);
    }

    public String getPrintUrl(DocumentModel doc, String defaultTheme) {
        Map<String, String> parameters = new HashMap<String, String>();
        int separatorIndex = defaultTheme.indexOf("/");
        if (separatorIndex != -1) {
            // defaultTheme includes the default page
            defaultTheme = defaultTheme.substring(0, separatorIndex);
            StringBuilder sb = new StringBuilder();
            sb.append(defaultTheme);
            sb.append("/print");
            parameters.put("theme", sb.toString());
        }
        return DocumentModelFunctions.documentUrl(null, doc, null, parameters,
                false, null);
    }

    public static HttpServletResponse getHttpServletResponse() {
        ServletResponse response = null;
        final FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            response = (ServletResponse) facesContext.getExternalContext().getResponse();
        }

        if (response != null && response instanceof HttpServletResponse) {
            return (HttpServletResponse) response;
        }
        return null;
    }

    public static HttpServletRequest getHttpServletRequest() {
        ServletRequest request = null;
        final FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            request = (ServletRequest) facesContext.getExternalContext().getRequest();
        }

        if (request != null && request instanceof HttpServletRequest) {
            return (HttpServletRequest) request;
        }
        return null;
    }

    public static void handleRedirect(HttpServletResponse response, String url)
            throws IOException {
        response.resetBuffer();
        response.sendRedirect(url);
        response.flushBuffer();
        getHttpServletRequest().setAttribute(
                NXAuthConstants.DISABLE_REDIRECT_REQUEST_KEY, true);
        FacesContext.getCurrentInstance().responseComplete();
    }
}
