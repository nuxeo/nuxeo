/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
@Operation(id = JsonStackToggleDisplayOperation.ID, category = Constants.CAT_EXECUTION, label = "Json Error Stack Display", description = "Toggle stack display in json response for all rest api"
        + " calls in Nuxeo", addToStudio = false, since = "6.0")
public class JsonStackToggleDisplayOperation {

    public static final String ID = "JsonStack.ToggleDisplay";

    @Param(name = "enableTrace", required = false)
    protected Boolean displayStack = null;

    @Context
    protected OperationContext ctx;

    protected boolean canManageStackDisplay() {
        NuxeoPrincipal principal = ctx.getPrincipal();
        return principal != null && (principal.isAdministrator());
    }

    @OperationMethod
    public boolean run() {
        JsonFactoryManager jsonFactoryManager = Framework.getService(JsonFactoryManager.class);
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
