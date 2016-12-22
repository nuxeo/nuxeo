/*
 * (C) Copyright 2013-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */

package org.nuxeo.ecm.automation.core.operations.traces;

import java.io.IOException;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.trace.Trace;
import org.nuxeo.ecm.automation.core.trace.TracerFactory;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.8
 */
@Operation(id = AutomationTraceGetOperation.ID, category = Constants.CAT_EXECUTION, label = "Traces.getTrace", description = "Retrieve trace associated to a Chain or an Operation", addToStudio = false)
public class AutomationTraceGetOperation {

    public static final String ID = "Traces.Get";

    @Param(name = "traceKey", required = false)
    protected String traceKey = null;

    @Param(name = "index", required = false)
    protected int index = -1;

    @Context
    protected OperationContext ctx;

    protected boolean canManageTraces() {
        NuxeoPrincipal principal = (NuxeoPrincipal) ctx.getPrincipal();
        return principal != null && principal.isAdministrator();
    }

    @OperationMethod
    public String run() throws IOException {

        if (!canManageTraces()) {
            return null;
        }

        TracerFactory tracerFactory = Framework.getLocalService(TracerFactory.class);

        if (traceKey == null) {
            Trace trace = tracerFactory.getLastErrorTrace();
            if (trace != null) {
                return tracerFactory.print(trace);
            } else {
                return "no previous error trace found";
            }
        } else {
            Trace trace = tracerFactory.getTrace(traceKey);
            if (trace != null) {
                return tracerFactory.print(trace);
            } else {
                return "no trace found";
            }
        }
    }
}
