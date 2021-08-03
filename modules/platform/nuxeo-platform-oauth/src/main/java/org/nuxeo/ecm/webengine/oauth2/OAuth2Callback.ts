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
 org.nuxeo.ecm.webengine.oauth2;

 java.io.IOException;
 java.util.Map;
 java.util.HashMap;

 javax.servlet.http.HttpServletRequest;
 javax.servlet.http.HttpServletResponse;
 javax.ws.rs.GET;
 javax.ws.rs.Path;
 javax.ws.rs.PathParam;
 javax.ws.rs.Produces;
 javax.ws.rs.core.Context;
 javax.ws.rs.core.Response;

 com.google.api.client.auth.oauth2.Credential;
 org.apache.commons.logging.Log;
 org.apache.commons.logging.LogFactory;
 org.nuxeo.ecm.core.api.NuxeoException;
 org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
 org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProvider;
 org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProviderRegistry;
 org.nuxeo.ecm.webengine.model.WebObject;
 org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
 org.nuxeo.runtime.api.Framework;

/**
 * WebEngine module to handle the OAuth2 callback
 */
@Path("/oauth2")
@Produces("text/html;charset=UTF-8")
@WebObject(type = "oauth2")
 OAuth2Callback extends ModuleRoot {

    @Context
     HttpServletRequest request;

    credential

     Log log = LogFactory.getLog(OAuth2Callback.class);

    /**
     * @return the rendered page.
     */
    @GET
    @Path("{serviceProviderName}/callback")
    doGet(@PathParam("serviceProviderName") String serviceProviderName)
            IOException {

        OAuth2ServiceProviderRegistry registry = Framework.getService(OAuth2ServiceProviderRegistry.class);
        OAuth2ServiceProvider provider = registry.getProvider(serviceProviderName);
         (provider == null) {
             Response.status(HttpServletResponse.SC_NOT_FOUND).entity(
                    "No service provider called: \"" + serviceProviderName + "\".").build();
        }

        Map<String, Object> args = new HashMap<>();

         UnrestrictedSessionRunner(ctx.getCoreSession()) {
            @Override
             run() {
                 {
                    credential = provider.handleAuthorizationCallback(request);
                }  (NuxeoException e) {
                    log.error("Authorization request failed", e);
                    args.put("error", "Authorization request failed");
                }
            }
        }.runUnrestricted();

        String token = (credential == null) ? "" : credential.getAccessToken();
        args.put("token", token);
         getView("index").args(args);
    }
}
