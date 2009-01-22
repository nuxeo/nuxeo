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

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.core.Manager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.platform.url.DocumentLocationImpl;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentLocation;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.runtime.api.Framework;

/**
 * TODO: document me.
 *
 * @author tiry
 * @author Florent Guillaume
 * @deprecated Most of the API should now use the url service.
 */
@Deprecated
@Name("URLMaker")
public class RestURLMaker {

    private static final Log log = LogFactory.getLog(RestURLMaker.class);

    @In(value = "org.jboss.seam.core.manager")
    public Manager conversationManager;

    @In(create = true)
    NavigationContext navigationContext;

    @In(create=true)
    private TypeManager typeManager;

    private URLPolicyService urlService;

    private String baseURL = "";

    private final Map<String, String> type2ViewMapper = new HashMap<String, String>();

    /**
     * @return url like http://server:8080/nuxeo/nxdoc/repo/uuid/view/tab
     */
    public String getDocumentFullURL(RepositoryLocation repo,
            DocumentRef docRef, String view, String tab,
            Boolean propagateConversation) {
        return getDocumentURL(repo, docRef, view, tab, propagateConversation);
    }

    protected URLPolicyService getUrlService() {
        if (urlService == null) {
            try {
                urlService = Framework.getService(URLPolicyService.class);
            } catch (Exception e) {
                log.error("Could not retrieve the URLPolicy service");
            }
        }
        return urlService;
    }

    /**
     * @return url like /nuxeo/nxdoc/repo/uuid/view/tab
     */
    public String getDocumentURL(RepositoryLocation repo, DocumentRef docRef,
            String view, String tab, Boolean propagateConversation) {
        String returnURL = "";

        if (FancyURLConfig.USE_FANCY_URL) {

            DocumentLocation docLoc = new DocumentLocationImpl(repo.getName(),
                    docRef);
            DocumentView dv = new DocumentViewImpl(docLoc, view);
            dv.addParameter("tabId", tab);

            URLPolicyService service = getUrlService();
            String baseUrl = BaseURL.getBaseURL();
            returnURL = service.getUrlFromDocumentView(dv, baseUrl);

            if (propagateConversation) {
                returnURL += '?' + getConversationPropagationSuffix();
            }
        } else {
            returnURL = getSimpleURL(repo, docRef, view, tab);
            if (propagateConversation) {
                returnURL += '&' + getConversationPropagationSuffix();
            }
        }

        return returnURL;
    }

    /**
     * @return url like http://server:8080/nuxeo
     */
    public String getDocumentBaseUrl() {
        return BaseURL.getServerURL() + "/" + BaseURL.getWebAppName();
    }

    public String getDocumentURL(DocumentRef docRef, String view, String tab) {
        RepositoryLocation repo = navigationContext.getCurrentServerLocation();
        return getDocumentURL(repo, docRef, view, tab, true);
    }

    public String getDocumentURL(DocumentRef docRef) {
        RepositoryLocation repo = navigationContext.getCurrentServerLocation();
        return getDocumentURL(repo, docRef, FancyURLConfig.DEFAULT_VIEW_ID,
                FancyURLConfig.DEFAULT_TAB_NAME, true);
    }

    public String getDocumentURLFromDocumentModel(DocumentModel doc)
            throws ClientException {
        if (null == doc) {
            throw new IllegalArgumentException("null document");
        }
        String jsfViewId = getDefaultViewId(doc);
        RepositoryLocation repo = navigationContext.getCurrentServerLocation();
        return getDocumentURL(repo, doc.getRef(), jsfViewId,
                FancyURLConfig.DEFAULT_TAB_NAME, true);
    }

    public String getDocumentURLFromDocumentModelWithTab(DocumentModel doc,
            String tabId) throws ClientException {
        String jsfViewId = getDefaultViewId(doc);
        RepositoryLocation repo = navigationContext.getCurrentServerLocation();
        return getDocumentURL(repo, doc.getRef(), jsfViewId, tabId, true);
    }

    public String getDocumentURL() throws ClientException {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        return getDocumentURLFromDocumentModel(currentDoc);
    }

    public String getDocumentURLForParentConversation() {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        String url = getDocumentURL(currentDoc.getRef());

        url = url.replace(getConversationPropagationSuffix(),
                getMainConversationPropagationSuffix());
        return url;
    }

    public String getDocumentPermLink() {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        RepositoryLocation repo = navigationContext.getCurrentServerLocation();
        return getDocumentURL(repo, currentDoc.getRef(),
                FancyURLConfig.DEFAULT_VIEW_ID,
                FancyURLConfig.DEFAULT_TAB_NAME, false);
    }

    public String switchTabForCurrentDocument(String tab) {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        RepositoryLocation repo = navigationContext.getCurrentServerLocation();
        return getDocumentURL(repo, currentDoc.getRef(),
                FancyURLConfig.DEFAULT_VIEW_ID, tab, true);
    }

    public String switchTabForCurrentDocument(String viewId, String tab) {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        RepositoryLocation repo = navigationContext.getCurrentServerLocation();
        return getDocumentURL(repo, currentDoc.getRef(), viewId, tab, true);
    }

    @Factory(value = "baseURL", scope = CONVERSATION)
    public String getBaseURL() {
        if (baseURL.equals("")) {
            baseURL = BaseURL.getBaseURL();
        }
        return baseURL;
    }

    @Factory(value = "needBaseURL", scope = CONVERSATION)
    public boolean getNeedBaseURL() {
        return FancyURLConfig.NEED_BASE_URL;
    }

    private String getSimpleURL(RepositoryLocation repo, DocumentRef docRef,
            String view, String tab) {
        String returnURL = "";

        returnURL += "/" + view + ".faces?";
        returnURL += FancyURLConfig.GET_URL_Server_Param + "=" + repo.getName()
                + "&";
        returnURL += FancyURLConfig.GET_URL_Doc_Param + "=" + docRef.toString()
                + "&";
        returnURL += FancyURLConfig.GET_URL_Tab_Param + "=" + tab;

        // XXX deal with encoding
        return returnURL;
    }

    private String getConversationPropagationSuffix() {
        String suffix = "";

        suffix += conversationManager.getConversationIdParameter() + "="
                + conversationManager.getCurrentConversationId();

/*      Not needed anymore ????
        if (conversationManager.isLongRunningConversation()) {
            suffix += "&"
                    + conversationManager.is
                    + "true";
        }*/

        return suffix;
    }

    private String getMainConversationPropagationSuffix() {
        String suffix = "";

        String cId = conversationManager.getCurrentConversationId();
        if (conversationManager.isNestedConversation()) {
            cId = conversationManager.getParentConversationId();
        }

        suffix += conversationManager.getConversationIdParameter() + "=" + cId;
        /*      Not needed anymore ????
        if (conversationManager.isLongRunningConversation()) {
            suffix += "&"
                    + conversationManager.getConversationIsLongRunningParameter()
                    + "true";
        }*/

        return suffix;
    }

    private String getDefaultViewId(DocumentModel doc) throws ClientException {
        String typeName = doc.getType();

        if (type2ViewMapper.containsKey(typeName)) {
            return type2ViewMapper.get(typeName);
        }

        Type type = typeManager.getType(typeName);
        if (type == null) {
            throw new ClientException(String.format(
                    "type %s for document '%s' is not a registered type",
                    typeName, doc.getTitle()));
        }
        String viewId = type.getDefaultView();

        // cache it for later use
        type2ViewMapper.put(typeName, viewId);

        return viewId;
    }
}
