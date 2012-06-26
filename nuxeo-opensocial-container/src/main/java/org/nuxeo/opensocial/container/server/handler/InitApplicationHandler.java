/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stéphane Fourrier
 */

package org.nuxeo.opensocial.container.server.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.customware.gwt.dispatch.server.ExecutionContext;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.opensocial.container.client.rpc.InitApplication;
import org.nuxeo.opensocial.container.client.rpc.InitApplicationResult;
import org.nuxeo.opensocial.container.server.utils.UrlBuilder;
import org.nuxeo.opensocial.container.server.webcontent.gadgets.opensocial.OpenSocialAdapter;
import org.nuxeo.opensocial.container.shared.layout.api.YUILayout;
import org.nuxeo.opensocial.container.shared.webcontent.OpenSocialData;
import org.nuxeo.opensocial.container.shared.webcontent.UserPref;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;
import org.nuxeo.opensocial.container.shared.webcontent.enume.DataType;
import org.nuxeo.opensocial.helper.OpenSocialGadgetHelper;

/**
 * @author Stéphane Fourrier
 */
public class InitApplicationHandler extends
        AbstractActionHandler<InitApplication, InitApplicationResult> {
    protected InitApplicationResult doExecute(InitApplication action,
            ExecutionContext context, CoreSession session) throws Exception {
        // TODO Get the permissions (for the moment we just put the perms for
        // the current space)
        // Has to be done later on for the layout's units and for the
        // webcontents
        Space space = getOrCreateSpace(action, session);

        // Get the layout from NUXEO
        YUILayout layout = space.getLayout().getLayout();

        // Get the webcontents in the layout
        List<WebContentData> list = space.readWebContents();
        Map<String, Map<String, Boolean>> permissions = space.getPermissions(list);
        Map<String, List<WebContentData>> webContents = new HashMap<String, List<WebContentData>>();

        for (WebContentData data : list) {
            updateCustomUserPreferences(data, action);
            if (webContents.containsKey(data.getUnitId())) {
                // webContents.get(data.getUnitId())
                // .add((int) data.getPosition(), data);
                webContents.get(data.getUnitId()).add(data);
            } else {
                List<WebContentData> temp = new ArrayList<WebContentData>();
                temp.add(data);
                webContents.put(data.getUnitId(), temp);
            }
        }
        return new InitApplicationResult(layout, webContents, permissions,
                space.getId());
    }

    protected Space getOrCreateSpace(InitApplication action, CoreSession session)
            throws ClientException {
        String spaceId = action.getSpaceId();
        if (spaceId != null && !spaceId.isEmpty()) {
            return getSpaceFromId(spaceId, session);
        } else {
            String documentContextId = action.getDocumentContextId();
            DocumentModel documentContext = null;
            if (documentContextId != null) {
                DocumentRef documentContextRef = new IdRef(documentContextId);
                if (session.exists(documentContextRef)) {
                    documentContext = session.getDocument(documentContextRef);
                }
            }
            SpaceManager spaceManager = getSpaceManager();
            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("userLanguage", action.getUserLanguage());
            return spaceManager.getSpace(action.getSpaceProviderName(),
                    session, documentContext, action.getSpaceName(), parameters);
        }
    }

    protected void updateCustomUserPreferences(WebContentData data,
            InitApplication action) {
        if (data instanceof OpenSocialData) {
            OpenSocialData openSocialData = (OpenSocialData) data;
            String documentLinkBuilder = action.getParameters().get(
                    "documentLinkBuilder");
            if (!StringUtils.isBlank(documentLinkBuilder)) {
                UserPref userPref = openSocialData.getUserPrefByName("documentLinkBuilder");
                if (userPref == null) {
                    userPref = new UserPref("documentLinkBuilder", DataType.STRING);
                    openSocialData.getUserPrefs().add(userPref);
                }
                userPref.setActualValue(documentLinkBuilder);
            }

            String activityLinkBuilder = action.getParameters().get(
                    "activityLinkBuilder");
            if (!StringUtils.isBlank(activityLinkBuilder)) {
                UserPref userPref = openSocialData.getUserPrefByName("activityLinkBuilder");
                if (userPref == null) {
                    userPref = new UserPref("activityLinkBuilder", DataType.STRING);
                    openSocialData.getUserPrefs().add(userPref);
                }
                userPref.setActualValue(activityLinkBuilder);
            }

            // recompute the frame url as the user prefs are changed
            try {
                openSocialData.computeFrameUrl();
            } catch (ClientException e) {
                // do nothing
            }
        }
    }

    public Class<InitApplication> getActionType() {
        return InitApplication.class;
    }

}
