/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nour Al Kotob
 */
package org.nuxeo.ecm.restapi.server.jaxrs.management;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.operations.services.workmanager.WorkManagerRunWorkInFailure;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 11.3
 */
@WebObject(type = ManagementObject.MANAGEMENT_OBJECT_PREFIX + "work-manager")
public class WorkManagerObject extends AbstractResource<ResourceTypeImpl> {

    public static final String TIMEOUT_SECONDS_PARAM_KEY = "timeoutSeconds";

    /**
     * Executes Works stored in the dead letter queue (DLQ) after failure.
     *
     * @param timeoutSeconds a timeout for the works to end.
     * @return The result of the rerun
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("run-works-in-failure")
    public Blob launch(@FormParam(TIMEOUT_SECONDS_PARAM_KEY) String timeoutSeconds) throws OperationException {
        AutomationService automationService = Framework.getService(AutomationService.class);
        try (OperationContext operationCtx = new OperationContext(this.ctx.getCoreSession())) {
            Map<String, Serializable> params = new HashMap<>();
            if (StringUtils.isNotBlank(timeoutSeconds)) {
                params.put(TIMEOUT_SECONDS_PARAM_KEY, timeoutSeconds);
            }
            return (Blob) automationService.run(operationCtx, WorkManagerRunWorkInFailure.ID, params);
        }
    }
}
