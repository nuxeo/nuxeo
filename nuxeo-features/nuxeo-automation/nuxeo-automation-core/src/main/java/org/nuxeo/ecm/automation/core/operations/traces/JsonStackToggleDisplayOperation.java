/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */

package org.nuxeo.ecm.automation.core.operations.traces;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.webengine.JsonFactoryManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 6.0
 */
@Operation(id = JsonStackToggleDisplayOperation.ID,
        category = Constants.CAT_EXECUTION, label = "Json Error Stack Display",
        description = "Toggle stack display in json response for all rest api" +
                " calls in Nuxeo", addToStudio = false, since = "6.0")
public class JsonStackToggleDisplayOperation {

    public static final String ID = "JsonStack.ToggleDisplay";

    @Param(name = "enableTrace", required = false)
    protected Boolean displayStack = null;

    @Context
    protected OperationContext ctx;

    protected boolean canManageStackDisplay() {
        NuxeoPrincipal principal = (NuxeoPrincipal) ctx.getPrincipal();
        return principal != null && (principal.isAdministrator());
    }

    @OperationMethod
    public boolean run() {
        JsonFactoryManager jsonFactoryManager = Framework.getLocalService
                (JsonFactoryManager.class);
        if (canManageStackDisplay()) {
            if (displayStack == null) {
                jsonFactoryManager.toggleStackDisplay();
            } else {
                if (displayStack != jsonFactoryManager.isStackDisplay()) {
                    jsonFactoryManager.toggleStackDisplay();
                }
            }
        }
        return jsonFactoryManager.isStackDisplay();
    }
}
