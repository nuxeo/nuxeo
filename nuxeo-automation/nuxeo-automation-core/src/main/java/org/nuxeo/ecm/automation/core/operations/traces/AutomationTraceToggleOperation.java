/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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

    @Context
    protected OperationContext ctx;

    protected boolean canManageTraces() {
        NuxeoPrincipal principal = (NuxeoPrincipal) ctx.getPrincipal();
        return principal != null && (principal.isAdministrator());
    }

    @OperationMethod
    public boolean run() {
        TracerFactory tracerFactory = Framework.getLocalService(TracerFactory.class);
        if (canManageTraces()) {
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
