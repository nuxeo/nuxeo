/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     "Guillaume Renard"
 */

package org.nuxeo.ecm.restapi.server.jaxrs.adapters;

import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.restapi.server.jaxrs.adapters.PaginableAdapter;
import org.nuxeo.ecm.restapi.server.jaxrs.usermanager.GroupObject;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.runtime.api.Framework;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @since 8.2
 */
@WebAdapter(name = GroupMemberGroupsAdapter.NAME, type = "GroupMemberGroups")
@Produces(MediaType.APPLICATION_JSON)
public class GroupMemberGroupsAdapter extends PaginableAdapter<NuxeoGroup> {

    public static final String NAME = "groups";
    public static final String PAGE_PROVIDER_NAME = "nuxeo_group_member_groups_listing";

    protected String query;

    @Override
    protected void initialize(Object... args) {
        super.initialize(args);
        query = ctx.getRequest().getParameter("q");
    }

    @Override
    protected PageProviderDefinition getPageProviderDefinition() {
        PageProviderService ppService = Framework.getService(PageProviderService.class);
        return ppService.getPageProviderDefinition(PAGE_PROVIDER_NAME);
    }

    @Override
    protected Object[] getParams() {
        return new Object[] { ((GroupObject) getTarget()).doGetArtifact(), query };
    }

}
