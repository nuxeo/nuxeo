package org.nuxeo.ecm.webengine.oauth2;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.platform.oauth2.providers.NuxeoOAuth2ServiceProvider;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProviderRegistry;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;

/**
 * The root entry for the WebEngine module.
 * 
 * @author nelson.silva
 */
@Path("/oauth2")
@Produces("text/html;charset=UTF-8")
@WebObject(type = "oauth2")
public class Root extends ModuleRoot {

    /** Global instance of the HTTP transport. */
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();

    /**
     * 
     * 
     * @param serviceProviderName
     * @param serviceProviderURL
     * @return the rendered page.
     * @throws Exception
     */
    @GET
    @Path("{serviceProviderName}/callback")
    public Object doGet(@PathParam("serviceProviderName")
    String serviceProviderName,
            @QueryParam(WEOAuthConstants.CODE_URL_PARAMETER)
            String code, @QueryParam(WEOAuthConstants.ERROR_URL_PARAMETER)
            String error, @DefaultValue("false")
            @QueryParam(WEOAuthConstants.INSTALLED_APP_PARAMETER)
            boolean isInstalledApp) throws Exception {

        // Checking if there was an error such as the user denied access
        if (error != null && error.length() > 0) {
            return Response.status(HttpServletResponse.SC_NOT_ACCEPTABLE).entity(
                    "There was an error: \"" + error + "\".").build();
        }
        // Checking conditions on the "code" URL parameter
        if (code == null || code.isEmpty()) {
            return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity(
                    "There is not code provided as QueryParam.").build();
        }
        NuxeoOAuth2ServiceProvider provider = getServiceProvider(serviceProviderName);
        if (provider == null) {
            return Response.status(HttpServletResponse.SC_NOT_FOUND).entity(
                    "No service provider called: \"" + serviceProviderName
                            + "\".").build();
        }

        AuthorizationCodeFlow flow = provider.getAuthorizationCodeFlow(
                HTTP_TRANSPORT, JSON_FACTORY);

        String redirectUri = ctx.getBaseURL()
                + WEOAuthConstants.getCallbackURL(serviceProviderName,
                        isInstalledApp);

        String userId = (isInstalledApp) ? WEOAuthConstants.INSTALLED_APP_USER_ID
                : getCurrentUsername();

        HttpResponse response = flow.newTokenRequest(code).setRedirectUri(
                redirectUri).executeUnparsed();
        TokenResponse tokenResponse = response.parseAs(TokenResponse.class);

        Credential credential = flow.createAndStoreCredential(tokenResponse,
                userId);

        return getView("index");
    }

    protected static NuxeoOAuth2ServiceProvider getServiceProvider(
            String serviceName) throws Exception {
        OAuth2ServiceProviderRegistry registry;
        try {
            registry = Framework.getService(OAuth2ServiceProviderRegistry.class);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Could not find OAuthServiceProviderRegistry service.", e);
        }
        NuxeoOAuth2ServiceProvider nuxeoServiceProvider = registry.getProvider(serviceName);

        return nuxeoServiceProvider;
    }

    protected String getCurrentUsername() {
        return ctx.getPrincipal().getName();
    }

}
