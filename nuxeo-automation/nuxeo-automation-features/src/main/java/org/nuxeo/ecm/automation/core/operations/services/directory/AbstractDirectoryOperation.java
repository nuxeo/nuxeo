/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.automation.core.operations.services.directory;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Base class for operations on directories.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public class AbstractDirectoryOperation {

    /**
     * Throws a {@link ClientException} if the current user on the {@code ctx}
     * cannot manage directories.
     */
    protected void validateCanManageDirectories(OperationContext ctx)
            throws ClientException {
        if (!canManageDirectories(ctx)) {
            throw new ClientException("Unauthorized user");
        }
    }

    protected boolean canManageDirectories(OperationContext ctx) {
        ActionManager actionManager = Framework.getLocalService(ActionManager.class);
        return actionManager.checkFilter("directoriesManagementAccess",
                createActionContext(ctx));
    }

    protected ActionContext createActionContext(OperationContext ctx) {
        ActionContext actionContext = new ActionContext();
        actionContext.setDocumentManager(ctx.getCoreSession());
        actionContext.setCurrentPrincipal((NuxeoPrincipal) ctx.getPrincipal());
        return actionContext;
    }
}
