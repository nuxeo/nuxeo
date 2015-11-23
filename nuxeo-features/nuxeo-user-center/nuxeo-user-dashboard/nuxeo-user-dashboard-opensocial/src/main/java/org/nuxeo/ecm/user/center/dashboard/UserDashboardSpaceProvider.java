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
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.user.center.dashboard;

import static org.nuxeo.ecm.spaces.api.Constants.SPACE_DOCUMENT_TYPE;
import static org.nuxeo.opensocial.container.shared.layout.enume.YUITemplate.YUI_ZT_50_50;

import java.util.Locale;
import java.util.Map;

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
import org.nuxeo.ecm.spaces.helper.WebContentHelper;
import org.nuxeo.opensocial.container.shared.layout.api.LayoutHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class UserDashboardSpaceProvider extends AbstractSpaceProvider {

    public static final String USER_DASHBOARD_SPACE_NAME = "userDashboardSpace";

    private static final Log log = LogFactory.getLog(UserDashboardSpaceProvider.class);

    @Override
    protected Space doGetSpace(CoreSession session,
            DocumentModel contextDocument, String spaceName,
            Map<String, String> parameters) throws SpaceException {
        try {
            return getOrCreateSpace(session, parameters);
        } catch (ClientException e) {
            log.error("Unable to create or get personal dashboard", e);
            return null;
        }
    }

    protected Space getOrCreateSpace(CoreSession session,
            Map<String, String> parameters) throws ClientException {
        String userWorkspacePath = getUserPersonalWorkspace(session).getPathAsString();
        DocumentRef spaceRef = new PathRef(userWorkspacePath,
                USER_DASHBOARD_SPACE_NAME);

        if (session.exists(spaceRef)) {
            DocumentModel existingSpace = session.getDocument(spaceRef);
            return existingSpace.getAdapter(Space.class);
        } else {
            DocumentModel model = session.createDocumentModel(
                    userWorkspacePath, USER_DASHBOARD_SPACE_NAME,
                    SPACE_DOCUMENT_TYPE);
            model.setPropertyValue("dc:title", "nuxeo user dashboard space");
            model.setPropertyValue("dc:description", "user dashboard space");
            model = session.createDocument(model);

            Space space = model.getAdapter(Space.class);
            initializeLayout(space);
            String userLanguage = parameters.get("userLanguage");
            Locale locale = userLanguage != null ? new Locale(userLanguage)
                    : null;
            initializeGadgets(space, session, locale);
            session.saveDocument(model);
            session.save();

            return model.getAdapter(Space.class);
        }
    }

    protected DocumentModel getUserPersonalWorkspace(CoreSession session)
            throws ClientException {
        try {
            UserWorkspaceService svc = Framework.getService(UserWorkspaceService.class);
            return svc.getCurrentUserPersonalWorkspace(session, null);
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    protected void initializeLayout(Space space) throws ClientException {
        space.initLayout(LayoutHelper.buildLayout(YUI_ZT_50_50, YUI_ZT_50_50,
                YUI_ZT_50_50));
    }

    protected void initializeGadgets(Space space, CoreSession session,
            Locale locale) throws ClientException {
        WebContentHelper.createOpenSocialGadget(space, session, locale,
                "userworkspaces", 0, 0, 0);
        WebContentHelper.createOpenSocialGadget(space, session, locale,
                "userdocuments", 0, 0, 1);
        WebContentHelper.createOpenSocialGadget(space, session, locale,
                "quicksearch", 0, 1, 0);
        WebContentHelper.createOpenSocialGadget(space, session, locale,
                "waitingfor", 0, 1, 1);
        WebContentHelper.createOpenSocialGadget(space, session, locale,
                "tasks", 0, 1, 2);
    }

    @Override
    public boolean isReadOnly(CoreSession session) {
        return true;
    }

}
