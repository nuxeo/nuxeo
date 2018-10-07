/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.automation.core.operations.services.directory;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ELActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Base class for operations on directories.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public class AbstractDirectoryOperation {

    protected void validateCanManageDirectories(OperationContext ctx) {
        if (!canManageDirectories(ctx)) {
            throw new NuxeoException("Unauthorized user");
        }
    }

    protected boolean canManageDirectories(OperationContext ctx) {
        ActionManager actionManager = Framework.getService(ActionManager.class);
        return actionManager.checkFilter("directoriesManagementAccess", createActionContext(ctx));
    }

    protected ActionContext createActionContext(OperationContext ctx) {
        ActionContext actionContext = new ELActionContext();
        actionContext.setDocumentManager(ctx.getCoreSession());
        actionContext.setCurrentPrincipal(ctx.getPrincipal());
        return actionContext;
    }
}
