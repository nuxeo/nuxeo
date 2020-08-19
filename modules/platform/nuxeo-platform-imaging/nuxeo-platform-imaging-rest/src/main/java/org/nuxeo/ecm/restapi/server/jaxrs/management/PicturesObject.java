/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */
package org.nuxeo.ecm.restapi.server.jaxrs.management;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.SYSTEM_USERNAME;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.platform.picture.recompute.RecomputeViewsAction;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * since 11.3
 */
@WebObject(type = ManagementObject.MANAGEMENT_OBJECT_PREFIX + "pictures")
@Produces(APPLICATION_JSON)
public class PicturesObject extends AbstractResource<ResourceTypeImpl> {

    public static final String PICTURES_DEFAULT_QUERY = "SELECT * FROM Document WHERE ecm:mixinType = 'Picture' AND picture:views/*/title IS NULL";

    /**
     * Recomputes picture views for the documents matching the given query or {@link #PICTURES_DEFAULT_QUERY} if not
     * provided.
     *
     * @param query a custom query to specify which pictures should be processed
     * @return the {@link BulkStatus} of the command
     */
    @POST
    @Path("recompute")
    public BulkStatus doPostPictures(@FormParam("query") String query) {
        String finalQuery = StringUtils.defaultIfBlank(query, PICTURES_DEFAULT_QUERY);
        BulkService bulkService = Framework.getService(BulkService.class);
        String commandId = bulkService.submit(new BulkCommand.Builder(RecomputeViewsAction.ACTION_NAME, finalQuery,
                SYSTEM_USERNAME).repository(ctx.getCoreSession().getRepositoryName())
                                .param(RecomputeViewsAction.PARAM_XPATH, "file:content")
                                .build());
        return bulkService.getStatus(commandId);
    }
}
