package org.nuxeo.ecm.platform.ui.web.keycloak;

import org.apache.catalina.connector.*;
import org.apache.catalina.core.StandardContext;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.adapters.AuthOutcome;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.representations.AccessToken;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.nuxeo.ecm.platform.ui.web.keycloak.KeycloakRequestAuthenticator.KEYCLOAK_ACCESS_TOKEN;

public class TestKeycloakAuthenticationPlugin extends NXRuntimeTestCase {

    private KeycloakRequestAuthenticator authenticatorMock = Mockito.mock(KeycloakRequestAuthenticator.class);

    private KeycloakAuthenticatorProvider providerMock = Mockito.mock(KeycloakAuthenticatorProvider.class);

    private NuxeoUserService nuxeoUserServiceMock = Mockito.mock(NuxeoUserService.class);

    private Request requestMock = Mockito.mock(Request.class);

    private Response responseMock = Mockito.mock(Response.class);

    private RequestFacade requestFacade = new RequestFacade(requestMock);

    private ResponseFacade responseFacade = new ResponseFacade(responseMock);

    private Connector connectorMock = Mockito.mock(Connector.class);

    private org.apache.coyote.Response coyoteResponseMock = new org.apache.coyote.Response();

    private static final String INVALID_BEARER_TOKEN = "Bearer invalid";

    @Before
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.platform.login.keycloak.test");
        deployContrib("org.nuxeo.ecm.platform.login.keycloak.test", "OSGI-INF/keycloak-descriptor-bundle.xml");

        Mockito.when(requestMock.getConnector()).thenReturn(connectorMock);
        Mockito.when(requestMock.getMethod()).thenReturn("GET");
        Mockito.when(requestMock.getRequestURI()).thenReturn("/foo/path/to/resource");
        Mockito.when(requestMock.getRequestURL()).thenReturn(new StringBuffer().append("https://example.com:443/foo/path/to/resource"));
        Mockito.when(requestMock.getScheme()).thenReturn("https");
        Mockito.when(requestMock.getServerName()).thenReturn("example.com");
        Mockito.when(requestMock.getServerPort()).thenReturn(443);
        Mockito.when(requestMock.getContextPath()).thenReturn("/foo");
        Mockito.when(requestMock.getContext()).thenReturn(new StandardContext());

        Mockito.when(connectorMock.getRedirectPort()).thenReturn(8080);
    }

    @Test
    public void testKeycloakBearerAuthenticationSucceeding() throws Exception {
        KeycloakAuthenticationPlugin keycloakAuthenticationPlugin = new KeycloakAuthenticationPlugin();
        initPlugin(keycloakAuthenticationPlugin);

        AccessToken accessToken = new AccessToken();
        accessToken.setEmail("username@example.com");
        AccessToken.Access realmAccess = new AccessToken.Access();
        realmAccess.addRole("user");
        accessToken.setRealmAccess(realmAccess);
        Mockito.when(requestMock.getAttribute(KEYCLOAK_ACCESS_TOKEN)).thenReturn(accessToken);
        Mockito.when(authenticatorMock.authenticate()).thenReturn(AuthOutcome.AUTHENTICATED);

        Mockito.when(providerMock.provide(any(HttpServletRequest.class), any(HttpServletResponse.class))).thenReturn(authenticatorMock);
        KeycloakDeployment deployment = new KeycloakDeployment();
        deployment.setResourceName("test");
        Mockito.when(providerMock.getResolvedDeployment()).thenReturn(deployment);

        Mockito.when(nuxeoUserServiceMock.findOrCreateUser(
                Matchers.any(KeycloakUserInfo.class),
                Matchers.anySetOf(String.class)
        )).thenReturn("");

        keycloakAuthenticationPlugin.setNuxeoUserService(nuxeoUserServiceMock);
        keycloakAuthenticationPlugin.setKeycloakAuthenticatorProvider(providerMock);

        UserIdentificationInfo identity = keycloakAuthenticationPlugin.handleRetrieveIdentity(requestFacade, responseMock);

        assertNotNull(identity);
        assertEquals("username@example.com", identity.getUserName());
    }

    @Test
    public void testKeycloakBearerAuthenticationFailing() throws Exception {
        KeycloakAuthenticationPlugin keycloakAuthenticationPlugin = new KeycloakAuthenticationPlugin();
        initPlugin(keycloakAuthenticationPlugin);

        // We'll check the response is marked committed
        Mockito.when(responseMock.getCoyoteResponse()).thenReturn(coyoteResponseMock);

        // No need to mock, just try the invalid bearer token
        Mockito.when(requestMock.getHeaders(Matchers.matches("Authorization"))).thenReturn(
                Collections.enumeration(Collections.singletonList(INVALID_BEARER_TOKEN))
        );

        UserIdentificationInfo identity = keycloakAuthenticationPlugin.handleRetrieveIdentity(requestFacade, responseFacade);

        assertNull(identity);

        Mockito.verify(responseMock).setStatus(401);
    }

    @Test
    public void testKeycloakSiteAuthenticationFailing() throws Exception {
        KeycloakAuthenticationPlugin keycloakAuthenticationPlugin = new KeycloakAuthenticationPlugin();
        initPlugin(keycloakAuthenticationPlugin);

        // We'll check the response is marked committed
        Mockito.when(responseMock.getCoyoteResponse()).thenReturn(coyoteResponseMock);

        // No need to mock, just try with NO bearer token
        UserIdentificationInfo identity = keycloakAuthenticationPlugin.handleRetrieveIdentity(requestFacade, responseFacade);

        assertNull(identity);

        Mockito.verify(responseMock).setStatus(302);
        Mockito.verify(responseMock)
                .setHeader(Matchers.matches("Location"),
                        Matchers.startsWith("https://127.0.0.1:8443/auth/realms/demo/protocol/openid-connect/auth?" +
                                "response_type=code&" +
                                "client_id=customer-portal&" +
                                "redirect_uri=https%3A%2F%2Fexample.com%3A443%2Ffoo%2Fpath%2Fto%2Fresource"));
    }

    @Test
    public void testKeycloakSiteLogout() throws Exception {
        KeycloakAuthenticationPlugin keycloakAuthenticationPlugin = new KeycloakAuthenticationPlugin();
        initPlugin(keycloakAuthenticationPlugin);

        // We'll check the response is marked committed
        Mockito.when(responseMock.getCoyoteResponse()).thenReturn(coyoteResponseMock);

        // No need to mock, just try with NO bearer token
        Boolean result = keycloakAuthenticationPlugin.handleLogout(requestFacade, responseFacade);

        assertNotNull(result);
        assertEquals(true, result);

        Mockito.verify(responseMock).sendRedirect("https://127.0.0.1:8443/auth/realms/demo/protocol/openid-connect/logout?redirect_uri=https://example.com:443/foo/nxstartup.faces");
    }

    private KeycloakAuthenticationPlugin initPlugin(KeycloakAuthenticationPlugin keycloakAuthenticationPlugin) {
        Map<String, String> parameters = new HashMap<>();
        // Add more configuration parameters in a future version
        parameters.put(KeycloakAuthenticationPlugin.KEYCLOAK_CONFIG_FILE_KEY, "keycloak.json");
        keycloakAuthenticationPlugin.initPlugin(parameters);
        return keycloakAuthenticationPlugin;
    }
}
