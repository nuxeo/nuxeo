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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.Request;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.NodesRegistrationManagement;
import org.keycloak.adapters.tomcat.CatalinaHttpFacade;
import org.nuxeo.ecm.platform.ui.web.auth.LoginScreenHelper;

/**
 * @since 7.4
 */

public class KeycloakAuthenticatorProvider {

    private final NodesRegistrationManagement nodesRegistrationManagement = new NodesRegistrationManagement();

    private final AdapterDeploymentContext deploymentContext;

    private KeycloakDeployment resolvedDeployment;

    public KeycloakAuthenticatorProvider(AdapterDeploymentContext deploymentContext) {
        this.deploymentContext = deploymentContext;
    }

    public KeycloakRequestAuthenticator provide(HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        DeploymentResult deploymentResult = new DeploymentResult(httpServletRequest, httpServletResponse).invokeOn(
                deploymentContext);

        if (!deploymentResult.isOk()) {
            return null;
        }

        resolvedDeployment = DeploymentResult.getKeycloakDeployment();
        Request request = deploymentResult.getRequest();
        CatalinaHttpFacade facade = deploymentResult.getFacade();

        // Register the deployment to refresh it
        nodesRegistrationManagement.tryRegister(resolvedDeployment);

        // And return authenticator
        return new KeycloakRequestAuthenticator(request, httpServletResponse, facade, resolvedDeployment);
    }

    public String logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        DeploymentResult deploymentResult = new DeploymentResult(httpServletRequest, httpServletResponse).invokeOn(
                deploymentContext);

        if (!deploymentResult.isOk()) {
            return null;
        }

        resolvedDeployment = DeploymentResult.getKeycloakDeployment();
        Request request = deploymentResult.getRequest();
        String redirecResource = getRedirectResource(request);

        return resolvedDeployment.getLogoutUrl().build().toString() + "?redirect_uri=" + redirecResource;
    }

    public KeycloakDeployment getResolvedDeployment() {
        return resolvedDeployment;
    }

    private String getRedirectResource(Request request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();
        return scheme + "://" + serverName + ":" + serverPort + contextPath + "/"
                + LoginScreenHelper.getStartupPagePath();
    }
}
