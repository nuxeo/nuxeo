/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Mariana Cedica <mcedica@nuxeo.com>
 */
package org.nuxeo.ecm.quota;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.quota.size.QuotaAware;
import org.nuxeo.ecm.quota.size.QuotaAwareDocumentFactory;

/**
 * Sets the quota on the user workspace if a global quota was set on all user
 * workspaces
 *
 * @since 5.7
 *
 */
public class QuotaUserWorkspaceListener implements EventListener {

    @Override
    public void handleEvent(Event event) throws ClientException {

        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }
        if (!DocumentEventTypes.USER_WORKSPACE_CREATED.equals(event.getName())) {
            return;
        }
        DocumentModel userWorkspace = ((DocumentEventContext) ctx).getSourceDocument();
        CoreSession session = userWorkspace.getCoreSession();
        DocumentModel userWorkspacesRoot = session.getDocument(userWorkspace.getParentRef());
        if (userWorkspacesRoot == null
                || !"UserWorkspacesRoot".equals(userWorkspacesRoot.getType())) {
            return;
        }
        QuotaAware qaUserWorkspaces = userWorkspacesRoot.getAdapter(QuotaAware.class);
        if (qaUserWorkspaces == null || qaUserWorkspaces.getMaxQuota() == -1L) {
            // no global quota activated on user workspaces
            return;
        }
        QuotaAware qa = userWorkspace.getAdapter(QuotaAware.class);
        if (qa == null) {
            qa = QuotaAwareDocumentFactory.make(userWorkspace, false);
        }
        // skip validation on other children quotas
        qa.setMaxQuota(qaUserWorkspaces.getMaxQuota(), true, true);
    }
}
