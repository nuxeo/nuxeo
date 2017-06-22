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
 *      Nelson Silva
 */
package org.nuxeo.ecm.webengine.oauth2;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.google.api.client.auth.oauth2.Credential;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProvider;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProviderRegistry;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;

/**
 * WebEngine module to handle the OAuth2 callback
 */
@Path("/oauth2")
@Produces("text/html;charset=UTF-8")
@WebObject(type = "oauth2")
public class OAuth2Callback extends ModuleRoot {

    @Context
    private HttpServletRequest request;

    Credential credential;

    private static final Log log = LogFactory.getLog(OAuth2Callback.class);

    /**
     * @param serviceProviderName
     * @return the rendered page.
     */
    @GET
    @Path("{serviceProviderName}/callback")
    public Object doGet(@PathParam("serviceProviderName") String serviceProviderName)
            throws IOException {

        OAuth2ServiceProviderRegistry registry = Framework.getService(OAuth2ServiceProviderRegistry.class);
        OAuth2ServiceProvider provider = registry.getProvider(serviceProviderName);
        if (provider == null) {
            return Response.status(HttpServletResponse.SC_NOT_FOUND).entity(
                    "No service provider called: \"" + serviceProviderName + "\".").build();
        }

        Map<String, Object> args = new HashMap<>();

        new UnrestrictedSessionRunner(ctx.getCoreSession()) {
            @Override
            public void run() {
                try {
                    credential = provider.handleAuthorizationCallback(request);
                } catch (NuxeoException e) {
                    log.error("Authorization request failed", e);
                    args.put("error", "Authorization request failed");
                }
            }
        }.runUnrestricted();

        String token = (credential == null) ? "" : credential.getAccessToken();
        args.put("token", token);
        return getView("index").args(args);
    }
}
