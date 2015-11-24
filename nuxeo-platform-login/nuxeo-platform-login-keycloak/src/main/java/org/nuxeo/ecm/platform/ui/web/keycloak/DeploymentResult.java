/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     François Maturel
 */

package org.nuxeo.ecm.platform.ui.web.keycloak;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.RequestFacade;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.tomcat.CatalinaHttpFacade;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;

/**
 *
 * @since 7.4
 */

public class DeploymentResult {
    private boolean isOk;

    private static KeycloakDeployment keycloakDeployment;

    private HttpServletRequest httpServletRequest;
    private HttpServletResponse httpServletResponse;
    private Request request;
    private CatalinaHttpFacade facade;

    public DeploymentResult(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        this.httpServletRequest = httpServletRequest;
        this.httpServletResponse = httpServletResponse;
    }

    boolean isOk() {
        return isOk;
    }

    public static KeycloakDeployment getKeycloakDeployment() {
        return keycloakDeployment;
    }

    public Request getRequest() {
        return request;
    }

    public CatalinaHttpFacade getFacade() {
        return facade;
    }

    public DeploymentResult invokeOn(AdapterDeploymentContext deploymentContext) {

        // In Tomcat, a HttpServletRequest and a HttpServletResponse are wrapped in a Facades
        request = unwrapRequest((RequestFacade) httpServletRequest);
        facade = new CatalinaHttpFacade(request, httpServletResponse);

        if (keycloakDeployment == null) {
            keycloakDeployment = deploymentContext.resolveDeployment(facade);
        }
        if (keycloakDeployment.isConfigured()) {
            isOk = true;
            return this;
        }
        isOk = false;
        return this;
    }

    /**
     * Get the wrapper {@link Request} hidden in a {@link RequestFacade} object
     *
     * @param requestFacade, the main RequestFacade object
     * @return the wrapper {@link Request} in {@link RequestFacade}
     */
    private Request unwrapRequest(RequestFacade requestFacade) {
        try {
            Field f = requestFacade.getClass().getDeclaredField("request");
            f.setAccessible(true); // grant access to (protected) field
            return (Request) f.get(requestFacade);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}