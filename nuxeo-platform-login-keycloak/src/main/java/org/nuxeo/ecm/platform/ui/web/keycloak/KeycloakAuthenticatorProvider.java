package org.nuxeo.ecm.platform.ui.web.keycloak;

import org.apache.catalina.connector.Request;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.NodesRegistrationManagement;
import org.keycloak.adapters.tomcat.CatalinaHttpFacade;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class KeycloakAuthenticatorProvider {

    private final NodesRegistrationManagement nodesRegistrationManagement = new NodesRegistrationManagement();

    private final AdapterDeploymentContext deploymentContext;

    private KeycloakDeployment resolvedDeployment;

    public KeycloakAuthenticatorProvider(AdapterDeploymentContext deploymentContext) {
        this.deploymentContext = deploymentContext;
    }

    public KeycloakRequestAuthenticator provide(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
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
        return scheme + "://" + serverName + ":" + serverPort + contextPath + "/" + NuxeoAuthenticationFilter.DEFAULT_START_PAGE;
    }
}