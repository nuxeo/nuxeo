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

package org.nuxeo.opensocial.container.server.handler.layout;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.opensocial.container.client.rpc.layout.action.DeleteYUIZone;
import org.nuxeo.opensocial.container.client.rpc.layout.result.DeleteYUIZoneResult;
import org.nuxeo.opensocial.container.server.handler.AbstractActionHandler;

import net.customware.gwt.dispatch.server.ExecutionContext;

/**
 * @author Stéphane Fourrier
 */
public class DeleteYUIZoneHandler extends
        AbstractActionHandler<DeleteYUIZone, DeleteYUIZoneResult> {

    protected DeleteYUIZoneResult doExecute(DeleteYUIZone action,
            ExecutionContext context, CoreSession session)
            throws ClientException {
        getSpaceFromId(action.getSpaceId(), session).getLayout().deleteZone(
                action.getZoneIndex());
        return new DeleteYUIZoneResult();
    }

    public Class<DeleteYUIZone> getActionType() {
        return DeleteYUIZone.class;
    }
}
