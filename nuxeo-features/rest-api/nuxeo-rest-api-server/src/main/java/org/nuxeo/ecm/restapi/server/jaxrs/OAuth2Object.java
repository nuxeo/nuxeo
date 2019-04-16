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

import static org.nuxeo.ecm.platform.oauth2.Constants.TOKEN_SERVICE;
import static org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token.SCHEMA;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.oauth2.clients.OAuth2Client;
import org.nuxeo.ecm.platform.oauth2.clients.OAuth2ClientService;
import org.nuxeo.ecm.platform.oauth2.providers.AbstractOAuth2UserEmailProvider;
import org.nuxeo.ecm.platform.oauth2.providers.NuxeoOAuth2ServiceProvider;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProvider;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProviderRegistry;
import org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token;
import org.nuxeo.ecm.platform.oauth2.tokens.OAuth2TokenStore;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.Credential;

/**
 * Endpoint to retrieve OAuth2 authentication data
 *
 * @since 8.4
 */
@WebObject(type = "oauth2")
public class OAuth2Object extends AbstractResource<ResourceTypeImpl> {

    public static final String TOKEN_DIR = "oauth2Tokens";

    /**
     * Lists all oauth2 service providers.
     *
     * @since 9.2
     */
    @GET
    @Path("provider")
    public List<NuxeoOAuth2ServiceProvider> getProviders(@Context HttpServletRequest request) {
        return getProviders();
    }

    /**
     * Retrieves oauth2 data for a given provider.
     */
    @GET
    @Path("provider/{providerId}")
    public Response getProvider(@PathParam("providerId") String providerId, @Context HttpServletRequest request) {
        return Response.ok(getProvider(providerId)).build();
    }

    /**
     * Creates a new OAuth2 service provider.
     *
     * @since 9.2
     */
    @POST
    @Path("provider")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addProvider(@Context HttpServletRequest request, NuxeoOAuth2ServiceProvider provider) {
        checkPermission(null);
        Framework.doPrivileged(() -> {
            OAuth2ServiceProviderRegistry registry = Framework.getService(OAuth2ServiceProviderRegistry.class);
            registry.addProvider(provider.getServiceName(), provider.getDescription(), provider.getTokenServerURL(),
                    provider.getAuthorizationServerURL(), provider.getUserAuthorizationURL(), provider.getClientId(),
                    provider.getClientSecret(), provider.getScopes(), provider.isEnabled());
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
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateProvider(@PathParam("providerId") String providerId, @Context HttpServletRequest request,
            NuxeoOAuth2ServiceProvider provider) {
        checkPermission(null);
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
    public Response deleteProvider(@PathParam("providerId") String providerId, @Context HttpServletRequest request) {
        checkPermission(null);
        getProvider(providerId);
        Framework.doPrivileged(() -> {
            OAuth2ServiceProviderRegistry registry = Framework.getService(OAuth2ServiceProviderRegistry.class);
            registry.deleteProvider(providerId);
        });
        return Response.noContent().build();
    }

    /**
     * Retrieves a valid access token for a given provider and the current user. If expired, the token will be
     * refreshed.
     */
    @GET
    @Path("provider/{providerId}/token")
    public Response getToken(@PathParam("providerId") String providerId, @Context HttpServletRequest request)
            throws IOException {

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
        Map<String, Object> result = new HashMap<>();
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
    public List<NuxeoOAuth2Token> getTokens(@Context HttpServletRequest request) {
        checkPermission(null);
        return getTokens();
    }

    /**
     * Retrieves an OAuth2 provider token.
     *
     * @since 10.2
     */
    @GET
    @Path("token/provider/{providerId}/user/{nxuser}")
    public Response getProviderToken(@PathParam("providerId") String providerId, @PathParam("nxuser") String nxuser,
            @Context HttpServletRequest request) {
        checkPermission(nxuser);
        NuxeoOAuth2ServiceProvider provider = getProvider(providerId);
        return Response.ok(getToken(provider, nxuser)).build();
    }

    /**
     * Retrieves an OAuth2 Token.
     *
     * @since 9.2
     * @deprecated since 10.2 Use {@link #getProviderToken(String, String, HttpServletRequest)} instead.
     */
    @Deprecated
    @GET
    @Path("token/{providerId}/{nxuser}")
    public Response getToken(@PathParam("providerId") String providerId, @PathParam("nxuser") String nxuser,
            @Context HttpServletRequest request) {
        return getProviderToken(providerId, nxuser, request);
    }

    /**
     * Updates an OAuth2 provider token.
     *
     * @since 10.2
     */
    @PUT
    @Path("token/provider/{providerId}/user/{nxuser}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateProviderToken(@PathParam("providerId") String providerId, @PathParam("nxuser") String nxuser,
            @Context HttpServletRequest request, NuxeoOAuth2Token token) {
        checkPermission(nxuser);
        NuxeoOAuth2ServiceProvider provider = getProvider(providerId);
        return Response.ok(updateToken(provider, nxuser, token)).build();
    }

    /**
     * Updates an OAuth2 Token.
     *
     * @since 9.2
     * @deprecated since 10.2 Use {@link #updateProviderToken(String, String, HttpServletRequest, NuxeoOAuth2Token)}
     *             instead.
     */
    @Deprecated
    @PUT
    @Path("token/{providerId}/{nxuser}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateToken(@PathParam("providerId") String providerId, @PathParam("nxuser") String nxuser,
            @Context HttpServletRequest request, NuxeoOAuth2Token token) {
        return updateProviderToken(providerId, nxuser, request, token);
    }

    /**
     * Deletes an OAuth2 provider token.
     *
     * @since 10.2
     */
    @DELETE
    @Path("token/provider/{providerId}/user/{nxuser}")
    public Response deleteProviderToken(@PathParam("providerId") String providerId, @PathParam("nxuser") String nxuser,
            @Context HttpServletRequest request) {
        checkPermission(nxuser);
        NuxeoOAuth2ServiceProvider provider = getProvider(providerId);
        deleteToken(getTokenDoc(provider, nxuser));
        return Response.noContent().build();
    }

    /**
     * Deletes an OAuth2 Token.
     *
     * @since 9.2
     * @deprecated since 10.2 Use {@link #deleteProviderToken(String, String, HttpServletRequest)} instead.
     */
    @Deprecated
    @DELETE
    @Path("token/{providerId}/{nxuser}")
    public Response deleteToken(@PathParam("providerId") String providerId, @PathParam("nxuser") String nxuser,
            @Context HttpServletRequest request) {
        return deleteProviderToken(providerId, nxuser, request);
    }

    /**
     * Retrieves all oauth2 provider tokens for the current user.
     *
     * @since 10.2
     */
    @GET
    @Path("token/provider")
    public List<NuxeoOAuth2Token> getProviderUserTokens(@Context HttpServletRequest request) {
        checkNotAnonymousUser();
        String nxuser = request.getUserPrincipal().getName();
        return getTokens(nxuser).stream() // filter: make sure no client tokens are retrieved
                                .filter(token -> token.getClientId() == null)
                                .collect(Collectors.toList());
    }

    /**
     * Retrieves all oauth2 client tokens for the current user.
     *
     * @since 10.2
     */
    @GET
    @Path("token/client")
    public List<NuxeoOAuth2Token> getClientUserTokens(@Context HttpServletRequest request) {
        checkNotAnonymousUser();
        String nxuser = request.getUserPrincipal().getName();
        return getTokens(nxuser).stream() // filter: make sure no provider tokens are retrieved
                                .filter(token -> token.getClientId() != null)
                                .collect(Collectors.toList());
    }

    /**
     * Retrieves a oauth2 client token.
     *
     * @since 10.2
     */
    @GET
    @Path("token/client/{clientId}/user/{nxuser}")
    public Response getClientToken(@PathParam("clientId") String clientId, @PathParam("nxuser") String nxuser,
            @Context HttpServletRequest request) {
        checkPermission(nxuser);
        OAuth2Client client = getClient(clientId);
        return Response.ok(getToken(client, nxuser)).build();
    }

    /**
     * Updates an OAuth2 client token.
     *
     * @since 10.2
     */
    @PUT
    @Path("token/client/{clientId}/user/{nxuser}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateClientToken(@PathParam("clientId") String clientId, @PathParam("nxuser") String nxuser,
            @Context HttpServletRequest request, NuxeoOAuth2Token token) {
        checkPermission(nxuser);
        OAuth2Client client = Framework.getService(OAuth2ClientService.class).getClient(clientId);
        return Response.ok(updateToken(client, nxuser, token)).build();
    }

    /**
     * Deletes a oauth2 client token.
     *
     * @since 10.2
     */
    @DELETE
    @Path("token/client/{clientId}/user/{nxuser}")
    public Response deleteClientToken(@PathParam("clientId") String clientId, @PathParam("nxuser") String nxuser,
            @Context HttpServletRequest request) {
        checkPermission(nxuser);
        OAuth2Client client = Framework.getService(OAuth2ClientService.class).getClient(clientId);
        deleteToken(getTokenDoc(client, nxuser));
        return Response.noContent().build();
    }

    /**
     * Retrieves oauth2 clients.
     *
     * @since 10.2
     */
    @GET
    @Path("client")
    public List<OAuth2Client> getClients(@Context HttpServletRequest request) {
        return Framework.getService(OAuth2ClientService.class).getClients();
    }

    /**
     * Retrieves a oauth2 client.
     *
     * @since 10.2
     */
    @GET
    @Path("client/{clientId}")
    public Response getClient(@PathParam("clientId") String clientId, @Context HttpServletRequest request) {
        OAuth2Client client = getClient(clientId);
        return Response.ok(client).build();
    }

    protected List<NuxeoOAuth2ServiceProvider> getProviders() {
        OAuth2ServiceProviderRegistry registry = Framework.getService(OAuth2ServiceProviderRegistry.class);
        return registry.getProviders()
                       .stream()
                       .filter(NuxeoOAuth2ServiceProvider.class::isInstance)
                       .map(provider -> (NuxeoOAuth2ServiceProvider) provider)
                       .collect(Collectors.toList());
    }

    protected NuxeoOAuth2ServiceProvider getProvider(String providerId) {
        OAuth2ServiceProvider provider = Framework.getService(OAuth2ServiceProviderRegistry.class)
                                                  .getProvider(providerId);
        if (provider == null || !(provider instanceof NuxeoOAuth2ServiceProvider)) {
            throw new WebResourceNotFoundException("Invalid provider: " + providerId);
        }
        return (NuxeoOAuth2ServiceProvider) provider;
    }

    protected List<NuxeoOAuth2Token> getTokens() {
        return getTokens((String) null);
    }

    protected List<NuxeoOAuth2Token> getTokens(String nxuser) {
        return Framework.doPrivileged(() -> {
            DirectoryService ds = Framework.getService(DirectoryService.class);
            try (Session session = ds.open(TOKEN_DIR)) {
                Map<String, Serializable> filter = new HashMap<>();
                if (nxuser != null) {
                    filter.put(NuxeoOAuth2Token.KEY_NUXEO_LOGIN, nxuser);
                }
                List<DocumentModel> docs = session.query(filter, Collections.emptySet(), Collections.emptyMap(), true,
                        0, 0);
                return docs.stream().map(NuxeoOAuth2Token::new).collect(Collectors.toList());
            }
        });
    }

    protected OAuth2Client getClient(String clientId) {
        OAuth2Client client = Framework.getService(OAuth2ClientService.class).getClient(clientId);
        if (client == null) {
            throw new WebResourceNotFoundException("Invalid client: " + clientId);
        }
        return client;
    }

    protected DocumentModel getTokenDoc(NuxeoOAuth2ServiceProvider provider, String nxuser) {
        Map<String, Serializable> filter = new HashMap<>();
        filter.put("serviceName", provider.getServiceName());
        filter.put(NuxeoOAuth2Token.KEY_NUXEO_LOGIN, nxuser);
        List<DocumentModel> tokens = Framework.doPrivileged(() -> {
            List<DocumentModel> entries = provider.getCredentialDataStore().query(filter);
            return entries.stream().filter(Objects::nonNull).collect(Collectors.toList());
        });
        if (tokens.size() > 1) {
            throw new NuxeoException("Found multiple " + provider.getId() + " accounts for " + nxuser);
        } else if (tokens.isEmpty()) {
            throw new WebResourceNotFoundException("No token found for provider: " + provider.getServiceName());
        } else {
            return tokens.get(0);
        }
    }

    protected DocumentModel getTokenDoc(OAuth2Client client, String nxuser) {
        Map<String, Serializable> filter = new HashMap<>();
        filter.put("clientId", client.getId());
        filter.put(NuxeoOAuth2Token.KEY_NUXEO_LOGIN, nxuser);
        OAuth2TokenStore tokenStore = new OAuth2TokenStore(TOKEN_SERVICE);
        List<DocumentModel> tokens = tokenStore.query(filter)
                                               .stream()
                                               .filter(Objects::nonNull)
                                               .collect(Collectors.toList());
        if (tokens.size() > 1) {
            throw new NuxeoException("Found multiple " + client.getId() + " accounts for " + nxuser);
        } else if (tokens.size() == 0) {
            throw new WebResourceNotFoundException("No token found for client: " + client.getId());
        } else {
            return tokens.get(0);
        }
    }

    protected NuxeoOAuth2Token getToken(NuxeoOAuth2ServiceProvider provider, String nxuser) {
        return new NuxeoOAuth2Token(getTokenDoc(provider, nxuser));
    }

    protected NuxeoOAuth2Token getToken(OAuth2Client client, String nxuser) {
        return new NuxeoOAuth2Token(getTokenDoc(client, nxuser));
    }

    protected NuxeoOAuth2Token updateToken(NuxeoOAuth2ServiceProvider provider, String nxuser, NuxeoOAuth2Token token) {
        updateTokenDoc(token, getTokenDoc(provider, nxuser));
        return getToken(provider, nxuser);
    }

    protected NuxeoOAuth2Token updateToken(OAuth2Client client, String nxuser, NuxeoOAuth2Token token) {
        updateTokenDoc(token, getTokenDoc(client, nxuser));
        return getToken(client, nxuser);
    }

    protected void updateTokenDoc(NuxeoOAuth2Token token, DocumentModel entry) {
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
    }

    protected void deleteToken(DocumentModel token) {
        Framework.doPrivileged(() -> {
            DirectoryService ds = Framework.getService(DirectoryService.class);
            try (Session session = ds.open(TOKEN_DIR)) {
                session.deleteEntry(token);
            }
        });
    }

    protected Credential getCredential(NuxeoOAuth2ServiceProvider provider, NuxeoOAuth2Token token) {
        return provider.loadCredential((provider instanceof AbstractOAuth2UserEmailProvider) ? token.getServiceLogin()
                : token.getNuxeoLogin());
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

    protected void checkPermission(String nxuser) {
        if (!hasPermission(nxuser)) {
            throw new WebSecurityException("You do not have permissions to perform this operation.");
        }
    }

    protected boolean hasPermission(String nxuser) {
        NuxeoPrincipal principal = getContext().getCoreSession().getPrincipal();
        return principal.isAdministrator() || (nxuser == null ? false : nxuser.equals(principal.getName()));
    }

    protected void checkNotAnonymousUser() {
        NuxeoPrincipal principal = getContext().getCoreSession().getPrincipal();
        if (principal.isAnonymous()) {
            throw new WebSecurityException("You do not have permissions to perform this operation.");
        }
    }

}
