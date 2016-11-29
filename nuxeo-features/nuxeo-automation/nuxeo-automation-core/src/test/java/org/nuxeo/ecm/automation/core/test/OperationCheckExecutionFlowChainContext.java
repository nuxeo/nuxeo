/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     vpasquier
 */
package org.nuxeo.ecm.automation.core.test;

import java.util.Map;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

@Operation(id = "oChainFlowCtx")
public class OperationCheckExecutionFlowChainContext {

    @Context
    OperationContext ctx;

    @Context
    CoreSession session;

    @OperationMethod
    public DocumentModel run(DocumentModel doc) throws Exception {
        // Check if chain parameters injected into execution flow operation
        // exists
        if (((Map) ctx.get(Constants.VAR_RUNTIME_CHAIN)).get("exampleKey2").equals("exampleValue2")) {
            return doc;
        }
        return null;
    }

}
