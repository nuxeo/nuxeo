/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Mariana Cedica <mcedica@nuxeo.com>
 */
package org.nuxeo.ecm.quota;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.quota.size.QuotaAware;
import org.nuxeo.ecm.quota.size.QuotaAwareDocumentFactory;

/**
 * Sets the quota on the user workspace if a global quota was set on all user workspaces
 *
 * @since 5.7
 */
public class QuotaUserWorkspaceListener implements EventListener {

    @Override
    public void handleEvent(Event event) {

        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }
        if (!"userWorkspaceCreated".equals(event.getName())) {
            return;
        }
        DocumentModel userWorkspace = ((DocumentEventContext) ctx).getSourceDocument();
        CoreSession session = userWorkspace.getCoreSession();
        DocumentModel userWorkspacesRoot = session.getDocument(userWorkspace.getParentRef());
        if (userWorkspacesRoot == null || !"UserWorkspacesRoot".equals(userWorkspacesRoot.getType())) {
            return;
        }
        QuotaAware qaUserWorkspaces = userWorkspacesRoot.getAdapter(QuotaAware.class);
        if (qaUserWorkspaces == null || qaUserWorkspaces.getMaxQuota() == -1L) {
            // no global quota activated on user workspaces
            return;
        }
        QuotaAware qa = QuotaAwareDocumentFactory.make(userWorkspace);
        // skip validation on other children quotas
        qa.setMaxQuota(qaUserWorkspaces.getMaxQuota(), true);
        qa.save();
    }
}
