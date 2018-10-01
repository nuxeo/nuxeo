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

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;

import java.util.List;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7.3
 */
@WebObject(type = "users")
@Produces(MediaType.APPLICATION_JSON)
public class UserRootObject extends AbstractUMRootObject<NuxeoPrincipal> {

    public static final String PAGE_PROVIDER_NAME = "nuxeo_principals_listing";

    @Override
    protected NuxeoPrincipal getArtifact(String id) {
        return um.getPrincipal(id);
    }

    @Override
    protected String getArtifactType() {
        return "user";
    }

    @Override
    protected void checkPrecondition(NuxeoPrincipal principal) {
        checkCurrentUserCanCreateArtifact(principal);
        checkPrincipalDoesNotAlreadyExists(principal, um);
        checkPrincipalHasAName(principal);
    }

    @Override
    protected NuxeoPrincipal createArtifact(NuxeoPrincipal principal) {
        um.createUser(principal.getModel());
        return um.getPrincipal(principal.getName());
    }

    private void checkPrincipalDoesNotAlreadyExists(NuxeoPrincipal principal, UserManager um) {
        NuxeoPrincipal user = um.getPrincipal(principal.getName());
        if (user != null) {
            throw new NuxeoException("User already exists", SC_CONFLICT);
        }
    }

    private void checkPrincipalHasAName(NuxeoPrincipal principal) {
        if (principal.getName() == null) {
            throw new NuxeoException("User MUST have a name", SC_BAD_REQUEST);
        }
    }

    @Override
    boolean isAPowerUserEditableArtifact(NuxeoPrincipal artifact) {
        return isAPowerUserEditableUser(artifact);
    }

    static boolean isAPowerUserEditableUser(NuxeoPrincipal user) {
        UserManager um = Framework.getService(UserManager.class);
        List<String> adminGroups = um.getAdministratorsGroups();
        for (String adminGroup : adminGroups) {
            if (user.getAllGroups().contains(adminGroup)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected PageProviderDefinition getPageProviderDefinition() {
        PageProviderService ppService = Framework.getService(PageProviderService.class);
        return ppService.getPageProviderDefinition(PAGE_PROVIDER_NAME);
    }

    @Override
    protected Object[] getParams() {
        return new Object[] { query };
    }

}
