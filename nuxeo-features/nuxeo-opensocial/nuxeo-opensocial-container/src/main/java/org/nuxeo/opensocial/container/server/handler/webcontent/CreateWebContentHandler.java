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

package org.nuxeo.opensocial.container.server.handler.webcontent;

import java.util.Locale;
import java.util.Map;

import net.customware.gwt.dispatch.server.ExecutionContext;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.opensocial.container.client.rpc.webcontent.action.CreateWebContent;
import org.nuxeo.opensocial.container.client.rpc.webcontent.result.CreateWebContentResult;
import org.nuxeo.opensocial.container.server.handler.AbstractActionHandler;
import org.nuxeo.opensocial.container.shared.webcontent.OpenSocialData;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;
import org.nuxeo.opensocial.gadgets.helper.GadgetI18nHelper;

/**
 * @author Stéphane Fourrier
 */
public class CreateWebContentHandler extends
        AbstractActionHandler<CreateWebContent, CreateWebContentResult> {

    public static final String GENERATE_TITLE_PARAMETER_NAME = "generateTitle";

    protected CreateWebContentResult doExecute(CreateWebContent action,
            ExecutionContext context, CoreSession session)
            throws ClientException {
        String spaceId = action.getSpaceId();
        Space space = getSpaceFromId(spaceId, session);
        WebContentData data = action.getData();
        data = generateTitle(data, action.getParameters(),
                action.getUserLanguage());
        data = updateGadgetPreferences(data, action, session);
        data = space.createWebContent(data);
        Map<String, Boolean> permissions = space.getPermissions(spaceId);
        return new CreateWebContentResult(data, permissions);
    }

    protected WebContentData generateTitle(WebContentData data,
            Map<String, String> parameters, String userLanguage) {
        String shouldGenerateTitle = parameters.get(GENERATE_TITLE_PARAMETER_NAME);
        if (!"false".equals(shouldGenerateTitle)) {
            String name = data instanceof OpenSocialData ? ((OpenSocialData) data).getGadgetName()
                    : data.getName();
            Locale locale = userLanguage != null ? new Locale(userLanguage)
                    : null;
            String title = GadgetI18nHelper.getI18nGadgetTitle(name, locale);
            data.setTitle(title);
        }
        return data;
    }

    protected WebContentData updateGadgetPreferences(WebContentData data,
            CreateWebContent action, CoreSession session)
            throws ClientException {
        Map<String, String> additionalPreferences = data.getAdditionalPreferences();
        additionalPreferences.put("nuxeoTargetRepository",
                action.getRepositoryName());

        String documentContextId = action.getDocumentContextId();
        DocumentModel documentContext = null;
        if (documentContextId != null) {
            DocumentRef documentContextRef = new IdRef(documentContextId);
            if (session.exists(documentContextRef)) {
                documentContext = session.getDocument(documentContextRef);
            }
        }
        if (documentContext != null) {
            additionalPreferences.put("nuxeoTargetContextPath",
                    documentContext.getPathAsString());
            additionalPreferences.put("nuxeoTargetContextObject",
                    documentContext.getType());
        }

        additionalPreferences.put("documentLinkBuilder",
                action.getParameters().get("documentLinkBuilder"));
        additionalPreferences.put("activityLinkBuilder",
                action.getParameters().get("activityLinkBuilder"));

        return data;
    }

    public Class<CreateWebContent> getActionType() {
        return CreateWebContent.class;
    }

}
