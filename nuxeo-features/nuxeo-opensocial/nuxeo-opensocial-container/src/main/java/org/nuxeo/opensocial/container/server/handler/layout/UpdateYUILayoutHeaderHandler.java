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
import org.nuxeo.opensocial.container.client.rpc.layout.action.UpdateYUILayoutHeader;
import org.nuxeo.opensocial.container.client.rpc.layout.result.UpdateYUILayoutHeaderResult;
import org.nuxeo.opensocial.container.server.handler.AbstractActionHandler;
import org.nuxeo.opensocial.container.shared.layout.api.YUIUnit;

import net.customware.gwt.dispatch.server.ExecutionContext;

/**
 * @author Stéphane Fourrier
 */
public class UpdateYUILayoutHeaderHandler
        extends
        AbstractActionHandler<UpdateYUILayoutHeader, UpdateYUILayoutHeaderResult> {

    protected UpdateYUILayoutHeaderResult doExecute(
            UpdateYUILayoutHeader action, ExecutionContext context,
            CoreSession session) throws ClientException {
        YUIUnit header = getSpaceFromId(action.getSpaceId(), session).getLayout().setHeader(
                action.getHeader());
        return new UpdateYUILayoutHeaderResult(header);
    }

    public Class<UpdateYUILayoutHeader> getActionType() {
        return UpdateYUILayoutHeader.class;
    }

}
