/*
 * (C) Copyright 2016 Nuxeo (http://nuxeo.com/) and others.
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
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;

import org.nuxeo.ecm.automation.server.jaxrs.RestOperationException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
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
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token.SCHEMA;

/**
 * Endpoint to retrieve OAuth2 authentication data
 * @since 8.4
 */
@WebObject(type = "oauth2")
public class OAuth2Object extends AbstractResource<ResourceTypeImpl> {

    public static final String APPLICATION_JSON_NXENTITY = "application/json+nxentity";

    public static final String TOKEN_DIR = "oauth2Tokens";

    /**
     * Lists all oauth2 service providers.
     *
     * @since 9.2
     */
    @GET
    @Path("provider")
    public List<NuxeoOAuth2ServiceProvider> getProviders(@Context HttpServletRequest request) throws IOException, RestOperationException {
        return getProviders();
    }

    /**
     * Retrieves oauth2 data for a given provider.
     */
    @GET
    @Path("provider/{providerId}")
    public Response getProvider(@PathParam("providerId") String providerId,
                                @Context HttpServletRequest request) throws IOException, RestOperationException {
        return Response.ok(getProvider(providerId)).build();
    }

    /**
     * Creates a new OAuth2 service provider.
     *
     * @since 9.2
     */
    @POST
    @Path("provider")
    @Consumes({ APPLICATION_JSON_NXENTITY, "application/json" })
    public Response addProvider(@Context HttpServletRequest request, NuxeoOAuth2ServiceProvider provider)
        throws IOException, RestOperationException {
        checkPermission();
        Framework.doPrivileged(() -> {
            OAuth2ServiceProviderRegistry registry = Framework.getService(OAuth2ServiceProviderRegistry.class);
            registry.addProvider(provider.getServiceName(),
                provider.getDescription(),
                provider.getTokenServerURL(),
                provider.getAuthorizationServerURL(),
                provider.getUserAuthorizationURL(),
                provider.getClientId(),
                provider.getClientSecret(),
                provider.getScopes(),
                provider.isEnabled());
        });
        return Response.ok(getProvider(provider.getServiceName())).build();
    }

    /**
     * Updates an OAuth2 service provider.
     *
     * @since 9.2
     */
    @PUT
    @Path("provider/{providerId}")
    @Consumes({ APPLICATION_JSON_NXENTITY, "application/json" })
    public Response updateProvider(@PathParam("providerId") String providerId,
                                   @Context HttpServletRequest request, NuxeoOAuth2ServiceProvider provider)
        throws IOException, RestOperationException {
        checkPermission();
        getProvider(providerId);
        Framework.doPrivileged(() -> {
            OAuth2ServiceProviderRegistry registry = Framework.getService(OAuth2ServiceProviderRegistry.class);
            registry.updateProvider(providerId, provider);
        });
        return Response.ok(getProvider(provider.getServiceName())).build();
    }

    /**
     * Deletes an OAuth2 service provider.
     *
     * @since 9.2
     */
    @DELETE
    @Path("provider/{providerId}")
    public Response deleteProvider(@PathParam("providerId") String providerId, @Context HttpServletRequest request)
        throws IOException, RestOperationException {
        checkPermission();
        getProvider(providerId);
        Framework.doPrivileged(() -> {
            OAuth2ServiceProviderRegistry registry = Framework.getService(OAuth2ServiceProviderRegistry.class);
            registry.deleteProvider(providerId);
        });
        return Response.noContent().build();
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

    /**
     * Retrieves all OAuth2 tokens.
     *
     * @since 9.2
     */
    @GET
    @Path("token")
    public List<NuxeoOAuth2Token> getTokens(@Context HttpServletRequest request)
        throws IOException, RestOperationException {
        checkPermission();
        return getTokens();
    }

    /**
     * Retrieves an OAuth2 Token.
     *
     * @since 9.2
     */
    @GET
    @Path("token/{providerId}/{nxuser}")
    public Response getToken(@PathParam("providerId") String providerId,
                             @PathParam("nxuser") String nxuser,
                             @Context HttpServletRequest request)
        throws IOException, RestOperationException {
        checkPermission();
        NuxeoOAuth2ServiceProvider provider = getProvider(providerId);
        return Response.ok(getToken(provider, nxuser)).build();
    }

    /**
     * Updates an OAuth2 Token.
     *
     * @since 9.2
     */
    @PUT
    @Path("token/{providerId}/{nxuser}")
    @Consumes({ APPLICATION_JSON_NXENTITY, "application/json" })
    public Response updateToken(@PathParam("providerId") String providerId,
                                @PathParam("nxuser") String nxuser,
                                @Context HttpServletRequest request, NuxeoOAuth2Token token)
        throws IOException, RestOperationException {
        checkPermission();
        NuxeoOAuth2ServiceProvider provider = getProvider(providerId);
        return Response.ok(updateToken(provider, nxuser, token)).build();
    }

    /**
     * Deletes an OAuth2 Token.
     *
     * @since 9.2
     */
    @DELETE
    @Path("token/{providerId}/{nxuser}")
    public Response deleteToken(@PathParam("providerId") String providerId,
                                @PathParam("nxuser") String nxuser,
                                @Context HttpServletRequest request) throws IOException, RestOperationException {
        checkPermission();
        NuxeoOAuth2ServiceProvider provider = getProvider(providerId);
        deleteToken(getTokenDoc(provider, nxuser));
        return Response.noContent().build();
    }

    protected List<NuxeoOAuth2ServiceProvider> getProviders() {
        OAuth2ServiceProviderRegistry registry = Framework.getService(OAuth2ServiceProviderRegistry.class);
        return registry.getProviders().stream()
            .filter(NuxeoOAuth2ServiceProvider.class::isInstance)
            .map(provider -> (NuxeoOAuth2ServiceProvider) provider)
            .collect(Collectors.toList());
    }

    protected NuxeoOAuth2ServiceProvider getProvider(String providerId) throws RestOperationException {
        OAuth2ServiceProvider provider = Framework.getService(OAuth2ServiceProviderRegistry.class)
            .getProvider(providerId);
        if (provider == null || !(provider instanceof NuxeoOAuth2ServiceProvider)) {
            RestOperationException err = new RestOperationException("Invalid provider: " + providerId);
            err.setStatus(HttpServletResponse.SC_NOT_FOUND);
            throw err;
        }
        return (NuxeoOAuth2ServiceProvider) provider;
    }

    protected List<NuxeoOAuth2Token> getTokens() {
        return getTokens((String)null);
    }

    protected List<NuxeoOAuth2Token> getTokens(String nxuser) {
        return Framework.doPrivileged(() -> {
            DirectoryService ds = Framework.getService(DirectoryService.class);
            try (Session session = ds.open(TOKEN_DIR)) {
                Map<String, Serializable> filter = new HashMap<>();
                if (nxuser != null) {
                    filter.put(NuxeoOAuth2Token.KEY_NUXEO_LOGIN, nxuser);
                }
                List<DocumentModel> docs = session.query(filter, Collections.emptySet(), Collections.emptyMap(),
                    true, 0, 0);
                return docs.stream().map(NuxeoOAuth2Token::new).collect(Collectors.toList());
            }
        });
    }

    protected DocumentModel getTokenDoc(NuxeoOAuth2ServiceProvider provider, String nxuser)
        throws RestOperationException {
        Map<String, Serializable> filter = new HashMap<>();
        filter.put("serviceName", provider.getServiceName());
        filter.put(NuxeoOAuth2Token.KEY_NUXEO_LOGIN, nxuser);
        List<DocumentModel> tokens = Framework.doPrivileged(() -> {
            List<DocumentModel> entries = provider.getCredentialDataStore().query(filter);
            return entries.stream().filter(Objects::nonNull).collect(Collectors.toList());
        });
        if (tokens.size() > 1) {
            throw new NuxeoException("Found multiple " + provider.getId() + " accounts for " + nxuser);
        } else if (tokens.size() == 0) {
            throw new RestOperationException("No token found for provider: " + provider.getId(), HttpServletResponse.SC_NOT_FOUND);
        } else {
            return tokens.get(0);
        }
    }

    protected NuxeoOAuth2Token getToken(NuxeoOAuth2ServiceProvider provider, String nxuser)
        throws RestOperationException {
        return new NuxeoOAuth2Token(getTokenDoc(provider, nxuser));
    }

    protected NuxeoOAuth2Token updateToken(NuxeoOAuth2ServiceProvider provider, String nxuser, NuxeoOAuth2Token token)
        throws RestOperationException {
        DocumentModel entry = getTokenDoc(provider, nxuser);
        entry.setProperty(SCHEMA, "serviceName", token.getServiceName());
        entry.setProperty(SCHEMA, "nuxeoLogin", token.getNuxeoLogin());
        entry.setProperty(SCHEMA, "clientId", token.getClientId());
        entry.setProperty(SCHEMA, "isShared", token.isShared());
        entry.setProperty(SCHEMA, "sharedWith", token.getSharedWith());
        entry.setProperty(SCHEMA, "serviceLogin", token.getServiceLogin());
        entry.setProperty(SCHEMA, "creationDate", token.getCreationDate());
        Framework.doPrivileged(() -> {
            DirectoryService ds = Framework.getService(DirectoryService.class);
            try (Session session = ds.open(TOKEN_DIR)) {
                session.updateEntry(entry);
            }
        });
        return getToken(provider, nxuser);
    }

    protected void deleteToken(DocumentModel token) throws RestOperationException {
        Framework.doPrivileged(() -> {
            DirectoryService ds = Framework.getService(DirectoryService.class);
            try (Session session = ds.open(TOKEN_DIR)) {
                session.deleteEntry(token);
            }
        });
    }

    protected Credential getCredential(NuxeoOAuth2ServiceProvider provider, NuxeoOAuth2Token token) {
        return provider.loadCredential(
            (provider instanceof AbstractOAuth2UserEmailProvider) ? token.getServiceLogin() : token.getNuxeoLogin());
    }

    protected Response buildResponse(StatusType status, Object obj) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String message = mapper.writeValueAsString(obj);

        return Response.status(status)
            .header("Content-Length", message.getBytes("UTF-8").length)
            .type(MediaType.APPLICATION_JSON + "; charset=UTF-8")
            .entity(message)
            .build();
    }

    protected void checkPermission() throws RestOperationException {
        if (!hasPermission()) {
            throw new RestOperationException("You do not have permissions to perform this operation.",
                HttpServletResponse.SC_FORBIDDEN);
        }
    }

    protected boolean hasPermission() {
        return ((NuxeoPrincipal) getContext().getCoreSession().getPrincipal()).isAdministrator();
    }

}
