/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 * Call Context Handler for Nuxeo authentication that extracts the relevant user
 * name.
 * <p>
 * Authentication happened earlier in the chain through Nuxeo's authentication
 * filter, and a JAAS context has already been set up.
 * <p>
 * There is no password available, as authentication is opaque and may use SSO.
 */
public class NuxeoAuthCallContextHandler implements CallContextHandler,
        Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public Map<String, String> getCallContextMap(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        HashMap<String, String> result = new HashMap<String, String>();
        if (principal != null) {
            result.put(CallContext.USERNAME, principal.getName());
        }
        return result;
    }

}
