/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 */

package org.nuxeo.ecm.automation.jsf;

import org.jboss.seam.contexts.Contexts;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.util.SeamComponentCallHelper;
import org.nuxeo.ecm.webapp.contentbrowser.DocumentActions;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;

/**
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
public class OperationHelper {

    public static boolean isSeamContextAvailable() {
        return Contexts.isSessionContextActive();
    }

    public static NavigationContext getNavigationContext() {
        return (NavigationContext) Contexts.getConversationContext().get(
                "navigationContext");
    }

    public static DocumentsListsManager getDocumentListManager() {
        return (DocumentsListsManager) Contexts.getSessionContext().get(
                "documentsListsManager");
    }

    public static ContentViewActions getContentViewActions() {
        return (ContentViewActions) Contexts.getConversationContext().get(
                "contentViewActions");
    }

    public static WebActions getWebActions() {
        return (WebActions) Contexts.getConversationContext().get("webActions");
    }

    public static DocumentActions getDocumentActions() {
        return (DocumentActions) SeamComponentCallHelper.getSeamComponentByName("documentActions");
    }

}
