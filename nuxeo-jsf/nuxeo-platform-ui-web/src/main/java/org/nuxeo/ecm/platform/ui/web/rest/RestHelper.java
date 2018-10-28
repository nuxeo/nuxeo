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

package org.nuxeo.ecm.platform.ui.web.rest;

import static org.jboss.seam.ScopeType.EVENT;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.Manager;
import org.jboss.seam.international.LocaleSelector;
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
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient WebActions webActions;

    protected DocumentView docView;

    protected String baseURL = "";

    @In(create = true)
    protected transient LocaleSelector localeSelector;

    /**
     * Sets current server location (core repository) and core document as provided by the document view.
     * <p>
     * Only sets current server location if the document reference is null.
     */
    @Begin(id = "#{conversationIdGenerator.currentOrNewMainConversationId}", join = true)
    public String initContextFromRestRequest(DocumentView docView) {
        String outcome = null;

        if (docView != null) {
            DocumentLocation docLoc = docView.getDocumentLocation();
            String serverName = docLoc.getServerName();
            if (serverName != null) {
                DocumentRef docRef = docLoc.getDocRef();
                RepositoryLocation repoLoc = new RepositoryLocation(serverName);
                if (docRef != null) {
                    if (docView.getParameter(WebActions.MAIN_TAB_ID_PARAMETER) == null
                            && !webActions.hasCurrentTabId(WebActions.MAIN_TABS_CATEGORY)) {
                        webActions.setCurrentTabId(WebActions.MAIN_TABS_CATEGORY, WebActions.DOCUMENTS_MAIN_TAB_ID);
                    }
                    outcome = navigationContext.navigateTo(repoLoc, docRef);
                } else {
                    navigationContext.setCurrentServerLocation(repoLoc);
                }
            }
            if (outcome == null) {
                outcome = docView.getViewId();
            }
        }

        return outcome;
    }

    public void setDocumentView(DocumentView docView) {
        this.docView = docView;
    }

    public DocumentView getNewDocumentView(String mainTabId) {
        DocumentView docView = null;
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument != null) {
            DocumentLocation docLoc = new DocumentLocationImpl(currentDocument);
            TypeInfo typeInfo = currentDocument.getAdapter(TypeInfo.class);
            Map<String, String> params = new HashMap<String, String>();
            if (currentDocument.isVersion()) {
                params.put("version", "true");
            }
            if (!StringUtils.isEmpty(mainTabId)) {
                params.put(WebActions.MAIN_TAB_ID_PARAMETER, WebActions.MAIN_TABS_CATEGORY + ":" + mainTabId);
            }
            // additional params will be set according to the url pattern,
            // calling getters on bindings.
            docView = new DocumentViewImpl(docLoc, typeInfo.getDefaultView(), params);
        }
        return docView;
    }

    public DocumentView getNewDocumentView() {
        return getNewDocumentView("documents");
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

    protected static String addConversationRequestParameters(String url, Manager conversationManager,
            String conversationId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(conversationManager.getConversationIdParameter(), conversationId);
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
            return addConversationRequestParameters(url, conversationManager, conversationId);
        }
    }

    public String getDocumentUrl(DocumentModel doc) {
        return DocumentModelFunctions.documentUrl(doc);
    }

    public String getDocumentUrl(DocumentModel doc, String viewId, boolean newConversation) {
        return DocumentModelFunctions.documentUrl(null, doc, viewId, null, newConversation, BaseURL.getBaseURL());
    }

    public String getDocumentUrl(String patternName, DocumentModel doc, String viewId, Map<String, String> parameters,
            boolean newConversation) {
        return DocumentModelFunctions.documentUrl(patternName, doc, viewId, parameters, newConversation,
                BaseURL.getBaseURL());
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

    public String doPrint(DocumentModel doc, String defaultTheme) throws IOException {
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
            parameters.put("page", sb.toString());
        }
        return DocumentModelFunctions.documentUrl(null, doc, null, parameters, false, BaseURL.getBaseURL());
    }

    public static HttpServletResponse getHttpServletResponse() {
        final FacesContext facesContext = FacesContext.getCurrentInstance();
        return facesContext == null ? null : (HttpServletResponse) facesContext.getExternalContext().getResponse();
    }

    public static HttpServletRequest getHttpServletRequest() {
        final FacesContext facesContext = FacesContext.getCurrentInstance();
        return (HttpServletRequest) facesContext.getExternalContext().getRequest();
    }

    public static void handleRedirect(HttpServletResponse response, String url) throws IOException {
        response.resetBuffer();
        response.sendRedirect(url);
        response.flushBuffer();
        getHttpServletRequest().setAttribute(NXAuthConstants.DISABLE_REDIRECT_REQUEST_KEY, Boolean.TRUE);
        FacesContext.getCurrentInstance().responseComplete();
    }

    /**
     * Returns the locale string.
     * <p>
     * Useful for url pattern bindings.
     *
     * @since 5.4.2
     */
    public String getLocaleString() {
        return localeSelector.getLocaleString();
    }

    public static final Pattern VALID_LOCALE = Pattern.compile("[A-Za-z0-9-_]*");

    /**
     * Sets the locale string if given string is not null and not empty, as well as on faces context view root in case
     * it was already created so that it holds the new locale for future lookups by JSF components.
     * <p>
     * Useful for url pattern bindings.
     */
    public void setLocaleString(String localeString) {
        // injected directly in JavaScript in a number of places, so should be sanitized
        if (!StringUtils.isBlank(localeString) && VALID_LOCALE.matcher(localeString).matches()) {
            localeSelector.setLocaleString(localeString.trim());
            FacesContext ctx = FacesContext.getCurrentInstance();
            if (ctx != null) {
                UIViewRoot viewRoot = ctx.getViewRoot();
                if (viewRoot != null) {
                    viewRoot.setLocale(localeSelector.getLocale());
                }
            }
        }
    }

}
