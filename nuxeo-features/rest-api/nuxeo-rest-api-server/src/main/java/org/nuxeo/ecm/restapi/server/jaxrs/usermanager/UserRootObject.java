/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.server.jaxrs.usermanager;

import java.util.List;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.runtime.api.Framework;

/**
 *
 *
 * @since 5.7.3
 */
@WebObject(type = "users")
@Produces({ MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_JSON + "+nxentity" })
public class UserRootObject extends AbstractUMRootObject<NuxeoPrincipal> {

    public static final String PAGE_PROVIDER_NAME = "nuxeo_principals_listing";

    @Override
    protected NuxeoPrincipal getArtifact(String id) throws ClientException {
        return um.getPrincipal(id);
    }

    @Override
    protected String getArtifactType() {
        return "user";
    }

    @Override
    protected void checkPrecondition(NuxeoPrincipal principal)
            throws ClientException {
        checkCurrentUserCanCreateArtifact(principal);
        checkPrincipalDoesNotAlreadyExists(principal, um);
        checkPrincipalHasAName(principal);
    }

    @Override
    protected NuxeoPrincipal createArtifact(NuxeoPrincipal principal)
            throws ClientException {
        um.createUser(principal.getModel());
        return um.getPrincipal(principal.getName());
    }

    private void checkPrincipalDoesNotAlreadyExists(NuxeoPrincipal principal,
            UserManager um) throws ClientException {
        NuxeoPrincipal user = um.getPrincipal(principal.getName());
        if (user != null) {
            throw new WebException("User already exists",
                    Response.Status.PRECONDITION_FAILED.getStatusCode());
        }
    }

    private void checkPrincipalHasAName(NuxeoPrincipal principal) {
        if (principal.getName() == null) {
            throw new WebException("User MUST have a name",
                    Response.Status.PRECONDITION_FAILED.getStatusCode());
        }
    }

    @Override
    boolean isAPowerUserEditableArtifact(NuxeoPrincipal artifact) {
        return isAPowerUserEditableUser(artifact);
    }

    static boolean isAPowerUserEditableUser(NuxeoPrincipal user) {
        UserManager um = Framework.getLocalService(UserManager.class);
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
        PageProviderService ppService = Framework.getLocalService(PageProviderService.class);
        return ppService.getPageProviderDefinition(PAGE_PROVIDER_NAME);
    }

    @Override
    protected Object[] getParams() {
        return new Object[] { query };
    }

}
