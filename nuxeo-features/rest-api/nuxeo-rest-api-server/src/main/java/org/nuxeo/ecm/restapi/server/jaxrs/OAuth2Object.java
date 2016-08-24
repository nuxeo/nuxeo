/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Gabriel Barata <gbarata@nuxeo.com>
 */
package org.nuxeo.ecm.restapi.server.jaxrs;

import com.google.api.client.auth.oauth2.Credential;
import org.codehaus.jackson.map.ObjectMapper;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;

import org.nuxeo.ecm.automation.server.jaxrs.RestOperationException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.oauth2.providers.AbstractOAuth2UserEmailProvider;
import org.nuxeo.ecm.platform.oauth2.providers.NuxeoOAuth2ServiceProvider;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProvider;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProviderRegistry;
import org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Endpoint to retrieve OAuth2 authentication data
 * @since 8.4
 */
@WebObject(type = "oauth2")
public class OAuth2Object extends AbstractResource<ResourceTypeImpl> {

    /**
     * Retrieves oauth2 data for a given provider.
     */
    @GET
    @Path("provider/{providerId}")
    public Response getProvider(@PathParam("providerId") String providerId,
                                @Context HttpServletRequest request) throws IOException, RestOperationException {

        NuxeoOAuth2ServiceProvider provider = getProvider(providerId);

        Map<String,Object> result = new HashMap<>();
        result.put("serviceName", provider.getServiceName());
        result.put("isAvailable", provider.isProviderAvailable());
        result.put("clientId", provider.getClientId());
        result.put("authorizationURL", provider.getClientId() == null ? null : provider.getAuthorizationUrl(request));

        String username = request.getUserPrincipal().getName();
        NuxeoOAuth2Token token = getToken(provider, username);
        boolean isAuthorized = (token != null);
        String login = isAuthorized ? token.getServiceLogin() : null;
        result.put("isAuthorized", isAuthorized);
        result.put("userId", login);

        return buildResponse(Status.OK, result);
    }

    /**
     * Retrieves a valid access token for a given provider and the current user.
     * If expired, the token will be refreshed.
     */
    @GET
    @Path("provider/{providerId}/token")
    public Response getToken(@PathParam("providerId") String providerId,
                             @Context HttpServletRequest request) throws IOException, RestOperationException {

        NuxeoOAuth2ServiceProvider provider = getProvider(providerId);

        String username = request.getUserPrincipal().getName();
        NuxeoOAuth2Token token = getToken(provider, username);
        if (token == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        Credential credential = getCredential(provider, token);

        if (credential == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        Long expiresInSeconds = credential.getExpiresInSeconds();
        if (expiresInSeconds != null && expiresInSeconds <= 0) {
            credential.refreshToken();
        }
        Map<String,Object> result = new HashMap<>();
        result.put("token", credential.getAccessToken());
        return buildResponse(Status.OK, result);
    }

    private NuxeoOAuth2Token getToken(NuxeoOAuth2ServiceProvider provider, String nxuser) {
        Map<String, Serializable> filter = new HashMap<>();
        filter.put("serviceName", provider.getId());
        filter.put(NuxeoOAuth2Token.KEY_NUXEO_LOGIN, nxuser);
        return Framework.doPrivileged(() -> {
            List<DocumentModel> entries = provider.getCredentialDataStore().query(filter);
            if (entries != null) {
                if (entries.size() > 1) {
                    throw new NuxeoException("Found multiple " + provider.getId() + " accounts for " + nxuser);
                } else if (entries.size() == 1) {
                    return new NuxeoOAuth2Token(entries.get(0));
                }
            }
            return null;
        });
    }

    private Credential getCredential(NuxeoOAuth2ServiceProvider provider, NuxeoOAuth2Token token) {
        return provider.loadCredential(
            (provider instanceof AbstractOAuth2UserEmailProvider) ? token.getServiceLogin() : token.getNuxeoLogin());
    }

    private NuxeoOAuth2ServiceProvider getProvider(String providerId) throws RestOperationException {
        OAuth2ServiceProvider provider = Framework.getService(OAuth2ServiceProviderRegistry.class)
            .getProvider(providerId);
        if (provider == null || !(provider instanceof NuxeoOAuth2ServiceProvider)) {
            RestOperationException err = new RestOperationException("Invalid provider: " + providerId);
            err.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            throw err;
        }
        return (NuxeoOAuth2ServiceProvider) provider;
    }

    private Response buildResponse(StatusType status, Object obj) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String message = mapper.writeValueAsString(obj);

        return Response.status(status)
            .header("Content-Length", message.getBytes("UTF-8").length)
            .type(MediaType.APPLICATION_JSON + "; charset=UTF-8")
            .entity(message)
            .build();
    }

}
