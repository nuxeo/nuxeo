/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.tokenauth;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.client.TokenCallback;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.runtime.api.Framework;

/**
 * Callback for token authentication.
 * <p>
 * The remote token retrieval is done using directly the
 * {@link TokenAuthenticationService} with the following parameters: userName,
 * applicationName, deviceName, deviceDescription and permission. The parameters
 * are passed to the default constructor. The token local storage is done in
 * memory.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.7
 */
public class TokenAuthenticationCallback implements TokenCallback {

    protected static final String USERNAME_KEY = "userName";

    protected static final String APPLICATION_NAME_KEY = "applicationName";

    protected static final String DEVICE_NAME_KEY = "deviceName";

    protected static final String DEVICE_DESCRIPTION_KEY = "deviceDescription";

    protected static final String PERMISSION_KEY = "permission";

    protected String token;

    protected String userName;

    protected String applicationName;

    protected String deviceName;

    protected String deviceDescription;

    protected String permission;

    public TokenAuthenticationCallback(String userName, String applicationName,
            String deviceName, String deviceDescription, String permission) {
        this.userName = userName;
        this.applicationName = applicationName;
        this.deviceName = deviceName;
        this.deviceDescription = deviceDescription;
        this.permission = permission;
    }

    @Override
    public String getLocalToken() {
        return token;
    }

    @Override
    public String getRemoteToken(Map<String, String> tokenParams) {

        String userName = tokenParams.get(USERNAME_KEY);
        String applicationName = tokenParams.get(APPLICATION_NAME_KEY);
        String deviceName = tokenParams.get(DEVICE_NAME_KEY);
        String deviceDescription = tokenParams.get(DEVICE_DESCRIPTION_KEY);
        String permission = tokenParams.get(PERMISSION_KEY);

        try {
            TokenAuthenticationService tokenAuthenticationService = Framework.getLocalService(TokenAuthenticationService.class);
            return tokenAuthenticationService.getToken(userName,
                    applicationName, deviceName, deviceDescription, permission);
        } catch (TokenAuthenticationException e) {
            throw new ClientRuntimeException(
                    "Error while trying to get remote token.", e);
        }
    }

    @Override
    public Map<String, String> getTokenParams() {
        Map<String, String> tokenParams = new HashMap<String, String>();
        tokenParams.put(USERNAME_KEY, userName);
        tokenParams.put(APPLICATION_NAME_KEY, applicationName);
        tokenParams.put(DEVICE_NAME_KEY, deviceName);
        tokenParams.put(DEVICE_DESCRIPTION_KEY, deviceDescription);
        tokenParams.put("permission", permission);
        return tokenParams;
    }

    @Override
    public void saveToken(String token) {
        this.token = token;
    }

}
