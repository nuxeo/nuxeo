/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     François Maturel
 */

package org.nuxeo.ecm.platform.ui.web.keycloak;

import java.lang.reflect.Field;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.RequestFacade;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.tomcat.CatalinaHttpFacade;

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
