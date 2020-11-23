/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Charles Boidot
 */
package org.nuxeo.ecm.restapi.server.jaxrs.management;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.SYSTEM_USERNAME;
import static org.nuxeo.ecm.platform.video.computation.RecomputeTranscodedVideosComputation.PARAM_CONVERSION_NAMES;
import static org.nuxeo.ecm.platform.video.computation.RecomputeTranscodedVideosComputation.PARAM_XPATH;
import static org.nuxeo.ecm.platform.video.computation.RecomputeVideoInfoComputation.RECOMPUTE_ALL_VIDEO_INFO;

import java.io.Serializable;
import java.util.List;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.platform.video.action.RecomputeVideoConversionsAction;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 11.5
 */
@WebObject(type = ManagementObject.MANAGEMENT_OBJECT_PREFIX + "videos")
@Produces(APPLICATION_JSON)
public class VideosObject extends AbstractResource<ResourceTypeImpl> {

    // By default we only recompute renditions for document without any renditions
    public static final String VIDEOS_DEFAULT_QUERY = "SELECT * FROM Document WHERE ecm:mixinType = 'Video' AND ecm:isProxy = 0 AND ecm:isVersion = 0 AND vid:transcodedVideos/0/name IS NULL";

    /**
     * Recomputes video renditions for the documents matching the given query or {@link #VIDEOS_DEFAULT_QUERY} if not
     * provided.
     *
     * @param query a custom query to specify which videos should be processed
     * @return the {@link BulkStatus} of the command
     */
    @POST
    @Path("recompute")
    public BulkStatus doPostVideos(@FormParam("query") String query,
            @FormParam("conversionNames") List<String> conversionNames,
            @FormParam("recomputeAllVideoInfo") Boolean recomputeAllVideoInfo) {
        String finalQuery = StringUtils.defaultIfBlank(query, VIDEOS_DEFAULT_QUERY);
        Boolean finalRecomputeAllVideoInfo = BooleanUtils.toBooleanDefaultIfNull(recomputeAllVideoInfo, false);
        BulkService bulkService = Framework.getService(BulkService.class);
        String commandId = bulkService.submit(new BulkCommand.Builder(RecomputeVideoConversionsAction.ACTION_NAME,
                finalQuery, SYSTEM_USERNAME).repository(ctx.getCoreSession().getRepositoryName())
                                            .param(PARAM_XPATH, "file:content")
                                            .param(RECOMPUTE_ALL_VIDEO_INFO, finalRecomputeAllVideoInfo)
                                            .param(PARAM_CONVERSION_NAMES, (Serializable) conversionNames)
                                            .build());
        return bulkService.getStatus(commandId);
    }

}
