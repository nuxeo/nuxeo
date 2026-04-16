/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.restapi.server.management;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.nuxeo.audit.bulk.CopyAuditAction.ACTION_NAME;
import static org.nuxeo.audit.bulk.CopyAuditAction.PARAMETER_BACKEND_NAME;
import static org.nuxeo.audit.scroll.AuditScroll.SCROLL_NAME;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.SYSTEM_USERNAME;
import static org.nuxeo.ecm.core.io.marshallers.NuxeoMediaType.TEXT_PLANT_UML;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.nuxeo.audit.api.AuditRouterIntrospection;
import org.nuxeo.audit.service.AuditRouter;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 2025.16
 */
@WebObject(type = ManagementObject.MANAGEMENT_OBJECT_PREFIX + "audit")
public class AuditObject extends AbstractResource<ResourceTypeImpl> {

    @POST
    @Path("/copy")
    public BulkStatus copyFromTo(@FormParam("from") String from, @FormParam("to") String to) {
        if (isBlank(from) || isBlank(to)) {
            throw new IllegalArgumentException("from and to cannot be blank");
        }
        String query = "SELECT * FROM " + from;
        var command = new BulkCommand.Builder(ACTION_NAME, query, SYSTEM_USERNAME).useQueryBuilderScroller()
                                                                                  .param(PARAMETER_BACKEND_NAME, to)
                                                                                  .setExclusive(true)
                                                                                  .scroller(SCROLL_NAME)
                                                                                  .build();
        var bulkService = Framework.getService(BulkService.class);
        String commandId = bulkService.submit(command);
        return bulkService.getStatus(commandId);
    }

    @GET
    @Path("/introspection")
    @Produces(TEXT_PLANT_UML)
    public AuditRouterIntrospection routing() {
        return Framework.getService(AuditRouter.class).getIntrospection();
    }
}
