/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.opensocial.dashboard;

import static org.nuxeo.ecm.spaces.api.Constants.SPACE_DOCUMENT_TYPE;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.spaces.api.AbstractSpaceProvider;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.opensocial.container.shared.layout.api.LayoutHelper;
import org.nuxeo.runtime.api.Framework;

public class DashboardSpaceProvider extends AbstractSpaceProvider {

    public static final String DASHBOARD_SPACE_NAME = "userDashboardSpace";

    private static final Log log = LogFactory.getLog(DashboardSpaceProvider.class);

    @Override
    protected Space doGetSpace(CoreSession session,
            DocumentModel contextDocument, String spaceName)
            throws SpaceException {
        try {
            return getOrCreateSpace(session);
        } catch (ClientException e) {
            log.error("Unable to create or get personal dashboard", e);
            return null;
        }
    }

    protected static DocumentModel getUserPersonalWorkspace(CoreSession session)
            throws ClientException {
        try {
            UserWorkspaceService svc = Framework.getService(UserWorkspaceService.class);
            return svc.getCurrentUserPersonalWorkspace(session, null);
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    protected static Space getOrCreateSpace(CoreSession session)
            throws ClientException {
        String userWorkspacePath = getUserPersonalWorkspace(session).getPathAsString();
        DocumentRef spaceRef = new PathRef(userWorkspacePath,
                DASHBOARD_SPACE_NAME);

        if (session.exists(spaceRef)) {
            DocumentModel existingSpace = session.getDocument(spaceRef);
            return existingSpace.getAdapter(Space.class);
        } else {
            DocumentModel model = session.createDocumentModel(
                    userWorkspacePath, DASHBOARD_SPACE_NAME, SPACE_DOCUMENT_TYPE);
            model.setPropertyValue("dc:title", "nuxeo dashboard space");
            model.setPropertyValue("dc:description", "dashboard space");
            model = session.createDocument(model);
            session.save();

            Space space = model.getAdapter(Space.class);
            space.initLayout(LayoutHelper.buildLayout(LayoutHelper.Preset.X_2_66_33));
            return space;
        }
    }

    @Override
    public boolean isReadOnly(CoreSession session) {
        return true;
    }

}
