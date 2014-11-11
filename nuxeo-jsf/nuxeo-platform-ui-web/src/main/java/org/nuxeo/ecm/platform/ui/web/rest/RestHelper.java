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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

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
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
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

    @Begin(id = "#{conversationIdGenerator.currentOrNewMainConversationId}", join = true)
    public String initContextFromRestRequest(DocumentView docView)
            throws ClientException {
        String outcome = null;

        if (docView != null) {
            DocumentLocation docLoc = docView.getDocumentLocation();
            outcome = navigationContext.navigateTo(new RepositoryLocation(
                    docLoc.getServerName()), docLoc.getDocRef());
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
        /* Not needed anymore ????
        if (conversationManager.isLongRunningConversation()) {
            params.put(conversationManager.getConversationIsLongRunningParameter(),
                "true");
        }*/
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

    @Factory(value = "baseURL", scope = ScopeType.CONVERSATION)
    public String getBaseURL() {
        if (baseURL.equals("")) {
            baseURL = BaseURL.getBaseURL();
        }
        return baseURL;
    }

}
