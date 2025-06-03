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
 *     Thomas Roger
 */

package org.nuxeo.ecm.restapi.server.jaxrs.management;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.nuxeo.launcher.config.ConfigurationConstants.PARAM_HTTP_PORT;

import java.util.Arrays;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 11.3
 */
@WebObject(type = "management")
public class ManagementObject extends AbstractResource<ResourceTypeImpl> {

    public static final String MANAGEMENT_OBJECT_PREFIX = "management/";

    protected static final String MANAGEMENT_API_HTTP_PORT_PROPERTY = "nuxeo.management.api.http.port";

    protected static final String MANAGEMENT_API_USER_PROPERTY = "nuxeo.management.api.user";

    protected static final String MANAGEMENT_API_GROUP_PROPERTY = "nuxeo.management.api.groups";

    @Context
    protected HttpServletRequest request;

    @Override
    protected void initialize(Object... args) {
        if (!requestIsOnConfiguredPort(request)) {
            throw new NuxeoException(HttpServletResponse.SC_NOT_FOUND);
        } else if (!isUserValid(request)) {
            throw new NuxeoException(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    @Path("{path}")
    public Object route(@PathParam("path") String path) {
        return newObject(MANAGEMENT_OBJECT_PREFIX + path);
    }

    protected boolean requestIsOnConfiguredPort(ServletRequest request) {
        int port = request.getLocalPort();
        String configPort = Framework.getProperty(MANAGEMENT_API_HTTP_PORT_PROPERTY,
                Framework.getProperty(PARAM_HTTP_PORT));
        return Integer.parseInt(configPort) == port;
    }

    protected boolean isUserValid(HttpServletRequest request) {
        if (request.getUserPrincipal() instanceof NuxeoPrincipal principal) {
            // if user is an administrator
            if (principal.isAdministrator()) {
                return true;
            }
            // if user is the configured one
            String managementUser = Framework.getProperty(MANAGEMENT_API_USER_PROPERTY);
            if (principal.getName().equals(managementUser)) {
                return true;
            }
            // if user belongs to configured group
            var managementGroups = Framework.getProperty(MANAGEMENT_API_GROUP_PROPERTY, EMPTY).split(",");
            if (Arrays.stream(managementGroups).anyMatch(principal::isMemberOf)) {
                return true;
            }
        }
        return false;
    }

}
