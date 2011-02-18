/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Laurent Doguin
 */
package org.nuxeo.ecm.usersettings.operations;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.usersettings.UserSettingsService;
import org.nuxeo.runtime.api.Framework;


@Operation(id = GetUserSettingsOperation.ID, category = Constants.CAT_USERS_GROUPS, label = "Get User Settings", description = "Get user settings providers")
public class GetUserSettingsOperation {
    public final static String ID = "User.getSettings";

    @Context
    protected OperationContext context;

    @Context
    protected CoreSession session;

    @Param(name = "type")
    protected String type;

    public UserSettingsService getDistributionService() {
        try {
            return Framework.getService(UserSettingsService.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @OperationMethod
    public DocumentModel getUserSettings() throws ClientException {
        return getDistributionService().getCurrentSettingsByType(session, type);
    }
}
