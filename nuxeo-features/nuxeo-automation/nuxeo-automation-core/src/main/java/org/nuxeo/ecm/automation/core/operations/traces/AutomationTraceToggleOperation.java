/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.trace.TracerFactory;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.8
 */
@Operation(id = AutomationTraceToggleOperation.ID, category = Constants.CAT_EXECUTION, label = "Traces.toggleRecording", description = "Toggle Automation call tracing (you can set the 'enableTrace' parameter if you want to explicitly set the traceEnable value", addToStudio = false)
public class AutomationTraceToggleOperation {

    public static final String ID = "Traces.ToggleRecording";

    @Param(name = "enableTrace", required = false)
    protected Boolean enableTrace = null;

    /**
     * @since 8.2
     */
    @Param(name = "readOnly", required = false)
    protected Boolean readOnly = false;

    @Context
    protected OperationContext ctx;

    protected boolean canManageTraces() {
        NuxeoPrincipal principal = ctx.getPrincipal();
        return principal != null && (principal.isAdministrator());
    }

    @OperationMethod
    public boolean run() {
        TracerFactory tracerFactory = Framework.getService(TracerFactory.class);
        if (canManageTraces() && !readOnly) {
            if (enableTrace == null) {
                tracerFactory.toggleRecording();
            } else {
                if (enableTrace != tracerFactory.getRecordingState()) {
                    tracerFactory.toggleRecording();
                }
            }
        }
        return tracerFactory.getRecordingState();
    }
}
