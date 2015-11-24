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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.Request;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.NodesRegistrationManagement;
import org.keycloak.adapters.tomcat.CatalinaHttpFacade;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;

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
        DeploymentResult deploymentResult = new DeploymentResult(httpServletRequest, httpServletResponse).invokeOn(deploymentContext);

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
        DeploymentResult deploymentResult = new DeploymentResult(httpServletRequest, httpServletResponse).invokeOn(deploymentContext);

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
                + NuxeoAuthenticationFilter.DEFAULT_START_PAGE;
    }
}