/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Cleanup Seam context
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
@Operation(id = DestroySeamContext.ID, category = Constants.CAT_UI, label = "Destroy Seam Context", description = "Cleanup up Seam context", aliases = {"WebUI.DestroySeamContext"})
public class DestroySeamContext {

    public static final String ID = "Seam.DestroyContext";

    @Param(name = "conversationId", required = false)
    protected String conversationId;

    @Context
    protected OperationContext context;

    @OperationMethod
    public void run() {
        SeamOperationFilter.handleAfterRun(context, conversationId);
    }

}
