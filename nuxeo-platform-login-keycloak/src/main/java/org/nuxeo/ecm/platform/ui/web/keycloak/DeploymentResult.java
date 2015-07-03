package org.nuxeo.ecm.platform.ui.web.keycloak;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.RequestFacade;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.tomcat.CatalinaHttpFacade;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;

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