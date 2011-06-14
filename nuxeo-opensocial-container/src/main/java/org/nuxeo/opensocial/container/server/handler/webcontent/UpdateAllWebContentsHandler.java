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

import java.util.List;
import java.util.Map.Entry;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.opensocial.container.client.rpc.webcontent.action.UpdateAllWebContents;
import org.nuxeo.opensocial.container.client.rpc.webcontent.result.UpdateAllWebContentsResult;
import org.nuxeo.opensocial.container.server.handler.AbstractActionHandler;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

import net.customware.gwt.dispatch.server.ExecutionContext;

/**
 * @author Stéphane Fourrier
 */
public class UpdateAllWebContentsHandler extends
        AbstractActionHandler<UpdateAllWebContents, UpdateAllWebContentsResult> {

    protected UpdateAllWebContentsResult doExecute(UpdateAllWebContents action,
            ExecutionContext context, CoreSession session)
            throws ClientException {
        Space space = getSpaceFromId(action.getSpaceId(), session);

        for (Entry<String, List<WebContentData>> unitEntry : action.getWebContents().entrySet()) {
            int position = 0;
            for (WebContentData webContent : unitEntry.getValue()) {
                webContent.setPosition(position);
                UpdateWebContentHandler.updateWebContent(webContent, null,
                        space);
                position++;
            }
        }
        return new UpdateAllWebContentsResult();
    }

    public Class<UpdateAllWebContents> getActionType() {
        return UpdateAllWebContents.class;
    }

}
