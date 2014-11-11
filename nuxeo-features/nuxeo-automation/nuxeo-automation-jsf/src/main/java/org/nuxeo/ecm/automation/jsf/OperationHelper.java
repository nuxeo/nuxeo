/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.automation.jsf;

import org.jboss.seam.contexts.Contexts;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;

/**
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
public class OperationHelper {

    public static NavigationContext getNavigationContext() {
        return (NavigationContext) Contexts.getConversationContext().get(
                "navigationContext");
    }

    public static DocumentsListsManager getDocumentListManager() {
        return (DocumentsListsManager) Contexts.getSessionContext().get(
                "documentsListsManager");
    }

    public static WebActions getWebActions() {
        return (WebActions) Contexts.getConversationContext().get("webActions");
    }
}
