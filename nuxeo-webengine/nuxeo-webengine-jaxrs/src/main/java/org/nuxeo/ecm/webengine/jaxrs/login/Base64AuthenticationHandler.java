/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs.login;

import java.util.Map;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.common.utils.Base64;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Base64AuthenticationHandler implements AuthenticationHandler {

    protected String realmName = "Nuxeo";

    @Override
    public void init(Map<String, String> properties) {
        String name = properties.get("realmName");
        if (name == null) {
            realmName = name;
        }
    }

    @Override
    public LoginContext handleAuthentication(HttpServletRequest request,
            HttpServletResponse response) throws LoginException {
        String[] login = retrieveBasicLogin(request);
        if (login != null) {
            return Framework.login(login[0], login[1]);
        }
        return null;
    }

    protected String[] retrieveBasicLogin(HttpServletRequest httpRequest) {
        String auth = httpRequest.getHeader("authorization");
        if (auth != null && auth.toLowerCase().startsWith("basic")) {
            int idx = auth.indexOf(' ');
            String b64userpassword = auth.substring(idx + 1);
            byte[] clearUp = Base64.decode(b64userpassword);
            String userpassword = new String(clearUp);
            String[] up = StringUtils.split(userpassword, ':', false);
            if (up.length != 2) {
                return null;
            }
            return up;
        }
        return null;
    }

    protected void handleLoginFailure(HttpServletRequest request, HttpServletResponse response) {
        String s = "Basic realm=\""+realmName+"\"";
        response.setHeader("WWW-Authenticate", s);
        response.setStatus(401);
    }


}
