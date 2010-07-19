/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.dam.platform.context.reuse;

import java.io.Serializable;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.Manager;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.platform.url.api.DocumentView;

import static org.jboss.seam.ScopeType.EVENT;
import static org.jboss.seam.annotations.Install.DEPLOYMENT;

/**
 * TODO: remove RestHelper : waiting Techlead solution (don't want
 * NavigationContext dependency) override RestHelper component without
 * NavigationContext
 *
 * @author Benjamin JALON
 */
@Name("restHelper")
@Scope(EVENT)
@Install(precedence = DEPLOYMENT)
public class RestHelper implements Serializable {

    private static final long serialVersionUID = 1L;

    private String baseURL = "";

    private DocumentView documentView;

    @Factory(value = "baseURL", scope = ScopeType.CONVERSATION)
    public String getBaseURL() {
        if (baseURL.equals("")) {
            baseURL = BaseURL.getBaseURL();
        }
        return baseURL;
    }

    public DocumentView getDocumentView() {
        return documentView;
    }

    public void setDocumentView(DocumentView documentView) {
        this.documentView = documentView;
    }

    public DocumentView getNewDocumentView() {
        // since 5.3 release this method have been added don't know why (BJA)
        return null;
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

    /**
     * Adds current conversation request parameters to the given url.
     *
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

}
