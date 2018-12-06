/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard <grenard@nuxeo.com>
 */
package org.nuxeo.ecm.platform.userworkspace.core.listener;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener to invalidate {@link UserWorkspaceService} when a domain is removed.
 *
 * @since 9.3
 */
public class InvalidateUserWorkspacesListener implements EventListener {

    @Override
    public void handleEvent(Event event) {
        EventContext ec = event.getContext();
        if (!(ec instanceof DocumentEventContext)) {
            return;
        }
        String evtName = event.getName();
        if (!DocumentEventTypes.DOCUMENT_REMOVED.equals(evtName)
                && !LifeCycleConstants.TRANSITION_EVENT.equals(evtName)) {
            // In 9.10, trashing a document results in changing its life cycle to deleted
            // If the life cycle changes on a domain, then maybe it is (un)trashed.
            // Since user workspaces location is based on the default domain's path
            // and (un)trashing a document alters its path,
            // we need to invalidate the user workspaces location
            return;
        }
        DocumentEventContext context = (DocumentEventContext) ec;
        DocumentModel doc = context.getSourceDocument();
        Path path = doc.getPath();
        if (path == null) {
            // Placeless document
            return;
        }
        if (path.segmentCount() == 1) {
            // the document is under root like are the domains
            // we need to invalidate user workspace location
            Framework.getService(UserWorkspaceService.class).invalidate();
        }
    }

}
