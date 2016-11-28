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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs.login;

import java.util.Map;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
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
    public LoginContext handleAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws LoginException {
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
            byte[] clearUp = Base64.decodeBase64(b64userpassword);
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
        String s = "Basic realm=\"" + realmName + "\"";
        response.setHeader("WWW-Authenticate", s);
        response.setStatus(401);
    }

}
