/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.tokenauth;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.client.TokenCallback;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Callback for token authentication.
 * <p>
 * The remote token retrieval is done using directly the {@link TokenAuthenticationService} with the following
 * parameters: userName, applicationName, deviceId, deviceDescription and permission. The parameters are passed to the
 * default constructor. The token local storage is done in memory.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.7
 */
public class TokenAuthenticationCallback implements TokenCallback {

    protected static final String USERNAME_KEY = "userName";

    protected static final String APPLICATION_NAME_KEY = "applicationName";

    protected static final String DEVICE_ID_KEY = "deviceId";

    protected static final String DEVICE_DESCRIPTION_KEY = "deviceDescription";

    protected static final String PERMISSION_KEY = "permission";

    protected String token;

    protected String userName;

    protected String applicationName;

    protected String deviceId;

    protected String deviceDescription;

    protected String permission;

    public TokenAuthenticationCallback(String userName, String applicationName, String deviceId,
            String deviceDescription, String permission) {
        this.userName = userName;
        this.applicationName = applicationName;
        this.deviceId = deviceId;
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
        String deviceId = tokenParams.get(DEVICE_ID_KEY);
        String deviceDescription = tokenParams.get(DEVICE_DESCRIPTION_KEY);
        String permission = tokenParams.get(PERMISSION_KEY);

        try {
            TokenAuthenticationService tokenAuthenticationService = Framework.getService(TokenAuthenticationService.class);
            String remoteToken = tokenAuthenticationService.acquireToken(userName, applicationName, deviceId,
                    deviceDescription, permission);
            // commit transaction so that token is committed in remote directory
            if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
            }
            return remoteToken;
        } catch (TokenAuthenticationException e) {
            e.addInfo("Error while trying to get remote token");
            throw e;
        }
    }

    @Override
    public Map<String, String> getTokenParams() {
        Map<String, String> tokenParams = new HashMap<String, String>();
        tokenParams.put(USERNAME_KEY, userName);
        tokenParams.put(APPLICATION_NAME_KEY, applicationName);
        tokenParams.put(DEVICE_ID_KEY, deviceId);
        tokenParams.put(DEVICE_DESCRIPTION_KEY, deviceDescription);
        tokenParams.put("permission", permission);
        return tokenParams;
    }

    @Override
    public void saveToken(String token) {
        this.token = token;
    }

}
