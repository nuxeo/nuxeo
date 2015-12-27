/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
        return (NavigationContext) Contexts.getConversationContext().get("navigationContext");
    }

    public static DocumentsListsManager getDocumentListManager() {
        return (DocumentsListsManager) Contexts.getSessionContext().get("documentsListsManager");
    }

    public static ContentViewActions getContentViewActions() {
        return (ContentViewActions) Contexts.getConversationContext().get("contentViewActions");
    }

    public static WebActions getWebActions() {
        return (WebActions) Contexts.getConversationContext().get("webActions");
    }

    public static DocumentActions getDocumentActions() {
        return (DocumentActions) SeamComponentCallHelper.getSeamComponentByName("documentActions");
    }

}
