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
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.server.jaxrs.usermanager;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;

/**
 * @since 5.7.3
 */
@WebObject(type = "user")
@Produces(MediaType.APPLICATION_JSON)
public class UserObject extends AbstractUMObject<NuxeoPrincipal> {

    // match everything until:
    // - '/@' for web adapters
    @Path("group/{groupName:((?:(?!(/@)).)*)}")
    public Object doGetUserToGroup(@PathParam("groupName") String groupName) {
        NuxeoGroup group = um.getGroup(groupName);
        if (group == null) {
            throw new WebResourceNotFoundException("Group not found");
        }

        return newObject("userToGroup", currentArtifact, group);
    }

    @Override
    protected NuxeoPrincipal updateArtifact(NuxeoPrincipal principal) {
        um.updateUser(principal.getModel());
        return um.getPrincipal(principal.getName());
    }

    @Override
    protected void deleteArtifact() {
        um.deleteUser(currentArtifact.getModel());
    }

    @Override
    protected boolean isAPowerUserEditableArtifact() {
        return UserRootObject.isAPowerUserEditableUser(currentArtifact);

    }

}
