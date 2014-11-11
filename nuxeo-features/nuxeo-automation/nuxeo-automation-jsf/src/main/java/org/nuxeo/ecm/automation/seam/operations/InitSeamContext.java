/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.automation.seam.operations;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;

/**
 * Initialise Seam Context so that next operation can use Seam components
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
@Operation(id = InitSeamContext.ID, category = Constants.CAT_UI, label = "Init Seam Context", description = "Initialize a Seam context (including Conversation if needed)")
public class InitSeamContext {

    public static final String ID = "Seam.InitContext";

    @Param(name = "conversationId", required = false)
    protected String conversationId;

    @Context
    protected OperationContext context;

    @OperationMethod
    public void run() throws Exception {
        SeamOperationFilter.handleBeforeRun(context, conversationId);
    }
}
