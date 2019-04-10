/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.bindings;

import java.io.Serializable;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.server.shared.CallContextHandler;

/**
 * Call Context Handler for Nuxeo authentication that extracts the relevant user name.
 * <p>
 * Configured as a "callContextHandler" servlet parameter in the AtomPub and JSON servlets.
 * <p>
 * Authentication happened earlier in the chain through Nuxeo's authentication filter, and a JAAS context has already
 * been set up.
 * <p>
 * There is no password available, as authentication is opaque and may use SSO.
 */
public class NuxeoAuthCallContextHandler implements CallContextHandler, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public Map<String, String> getCallContextMap(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        Map<String, String> result = new HashMap<String, String>();
        if (principal != null) {
            result.put(CallContext.USERNAME, principal.getName());
        }
        return result;
    }

}
