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
 *     Nelson Silva <nsilva@nuxeo.com>
 */
package org.nuxeo.ecm.restapi.server.jaxrs.login.tokenauth;

import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.tokenauth.io.AuthenticationToken;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Token Object
 *
 * @since 8.3
 */
@WebObject(type = "token")
@Produces(MediaType.APPLICATION_JSON)
public class AuthenticationTokensObject extends AbstractResource<ResourceTypeImpl> {

    private TokenAuthenticationService service;

    @Override
    protected void initialize(Object... args) {
        service = Framework.getService(TokenAuthenticationService.class);
    }

    @GET
    public List<AuthenticationToken> getTokens(@QueryParam("application") String applicationName) {
        DocumentModelList tokens = service.getTokenBindings(getCurrentUser().getName(), applicationName);
        return tokens.stream().map(this::asAuthenticationToken).collect(Collectors.toList());
    }

    @POST
    @Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN })
    public Response createToken(@QueryParam("application") String applicationName,
            @QueryParam("deviceId") String deviceId, @QueryParam("deviceDescription") String deviceDescription,
            @QueryParam("permission") String permission, @HeaderParam("accept") String acceptType) {
        String username = getCurrentUser().getName();
        String token = service.acquireToken(username, applicationName, deviceId, deviceDescription, permission);
        String body = token;
        MediaType cType = MediaType.TEXT_PLAIN_TYPE;
        List<MediaType> acceptList = accepts(acceptType);
        if (acceptList.isEmpty() || acceptList.stream().anyMatch(type -> MediaType.APPLICATION_JSON_TYPE.isCompatible(type))) {
           body = String.format("\"%s\"", token);
           cType = MediaType.APPLICATION_JSON_TYPE;
        }
        return Response.ok(body).type(cType).status(Response.Status.CREATED).build();
    }

    @DELETE
    @Path("{token}")
    public void deleteToken(@PathParam("token") String tokenId) {
        if (tokenId == null) {
            return;
        }
        service.revokeToken(tokenId);
    }

    private NuxeoPrincipal getCurrentUser() {
        return getContext().getCoreSession().getPrincipal();
    }

    private AuthenticationToken asAuthenticationToken(DocumentModel entry) {
        Map<String, Object> props = entry.getProperties("authtoken");
        AuthenticationToken token = new AuthenticationToken(
                (String) props.get("token"),
                (String) props.get("userName"),
                (String) props.get("applicationName"),
                (String) props.get("deviceId"),
                (String) props.get("deviceDescription"),
                (String) props.get("permission"));
        token.setCreationDate((Calendar) props.get("creationDate"));
        return token;
    }

    private List<MediaType> accepts(String value) {
        // if there is no Accept header it means that all media types are
        // acceptable
        if (StringUtils.isBlank(value)) {
            return Collections.singletonList(MediaType.WILDCARD_TYPE);
        }
        List<MediaType> list = new LinkedList<MediaType>();
        String[] mediaTypes = StringUtils.split(value, ",");
        for (String mediaRange : mediaTypes) {
            mediaRange = mediaRange.trim();
            if (mediaRange.length() == 0) {
                continue;
            }
            try {
                list.add(MediaType.valueOf(mediaRange));
            } catch (IllegalArgumentException iex) {
                // pass
            }
        }
        return list;
    }
}
