/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.keycloak;

import org.apache.commons.lang3.StringUtils;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.AuthOutcome;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.representations.AccessToken;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPluginLogoutExtension;
import org.nuxeo.runtime.api.Framework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.nuxeo.ecm.platform.ui.web.keycloak.KeycloakUserInfo.KeycloakUserInfoBuilder.aKeycloakUserInfo;

public class KeycloakAuthenticationPlugin implements NuxeoAuthenticationPlugin, NuxeoAuthenticationPluginLogoutExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakAuthenticationPlugin.class);

    private static final String PROTOCOL_CLASSPATH = "classpath:";

    public static final String KEYCLOAK_CONFIG_FILE_KEY = "keycloakConfigFilename";
    private String keycloakConfigFile = PROTOCOL_CLASSPATH + "keycloak.json";

    private NuxeoUserService nuxeoUserService = NuxeoUserService.getInstance();

    private KeycloakAuthenticatorProvider keycloakAuthenticatorProvider;

    public void initPlugin(Map<String, String> parameters) {
        LOGGER.info("INITIALIZE KEYCLOAK");

        if (parameters.containsKey(KEYCLOAK_CONFIG_FILE_KEY)) {
            keycloakConfigFile = PROTOCOL_CLASSPATH + parameters.get(KEYCLOAK_CONFIG_FILE_KEY);
        }

        InputStream is = loadKeycloakConfigFile();
        KeycloakDeployment kd = KeycloakNuxeoDeployment.build(is);
        keycloakAuthenticatorProvider = new KeycloakAuthenticatorProvider(new AdapterDeploymentContext(kd));
        LOGGER.info("Keycloak is using a per-deployment configuration loaded from: " + keycloakConfigFile);
    }

    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return Boolean.TRUE;
    }

    public Boolean handleLoginPrompt(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String baseURL) {
        return Boolean.TRUE;
    }

    public List<String> getUnAuthenticatedURLPrefix() {
        // There are no unauthenticated URLs associated to login prompt.
        // If user is not authenticated, this plugin will have to redirect user to the keycloak sso login prompt
        return null;
    }

    public UserIdentificationInfo handleRetrieveIdentity(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        LOGGER.debug("KEYCLOAK will handle identification");

        KeycloakRequestAuthenticator authenticator = keycloakAuthenticatorProvider.provide(httpRequest, httpResponse);
        KeycloakDeployment deployment = keycloakAuthenticatorProvider.getResolvedDeployment();
        String keycloakNuxeoApp = deployment.getResourceName();

        AuthOutcome outcome = authenticator.authenticate();

        if (outcome == AuthOutcome.AUTHENTICATED) {
            AccessToken token = (AccessToken) httpRequest.getAttribute(KeycloakRequestAuthenticator.KEYCLOAK_ACCESS_TOKEN);

            KeycloakUserInfo keycloakUserInfo = getKeycloakUserInfo(token);

            nuxeoUserService.findOrCreateUser(keycloakUserInfo, getRoles(token, keycloakNuxeoApp));
            return keycloakUserInfo;
        }
        return null;
    }

    @Override
    public Boolean handleLogout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        LOGGER.debug("KEYCLOAK will handle logout");

        String uri = keycloakAuthenticatorProvider.logout(httpRequest, httpResponse);
        try {
            httpResponse.sendRedirect(uri);
        } catch (IOException e) {
            String message = "Could note handle logout with URI: " + uri;
            LOGGER.error(message);
            throw new RuntimeException(message);
        }
        return Boolean.TRUE;
    }

    /**
     * Get keycloak user's information from authentication token
     *
     * @param token the keycoak authentication token
     * @return keycloak user's information
     */
    private KeycloakUserInfo getKeycloakUserInfo(AccessToken token) {
        return aKeycloakUserInfo()
                // Required
                .withUserName(token.getEmail())
                        // Optional
                .withFirstName(token.getGivenName())
                .withLastName(token.getFamilyName())
                .withCompany(token.getPreferredUsername())
                .withAuthPluginName("KEYCLOAK_AUTH")
                        // The password is randomly generated has we won't use it
                .withPassword(UUID.randomUUID().toString())
                .build();
    }

    /**
     * Get keycloak user's roles from authentication token
     *
     * @param token the keycoak authentication token
     * @param keycloakNuxeoApp the keycoak resource name
     * @return keycloak user's roles
     */
    private Set<String> getRoles(AccessToken token, String keycloakNuxeoApp) {
        Set<String> allRoles = new HashSet<>();
        allRoles.addAll(token.getRealmAccess().getRoles());
        AccessToken.Access nuxeoResource = token.getResourceAccess(keycloakNuxeoApp);
        if (nuxeoResource != null) {
            Set<String> nuxeoRoles = nuxeoResource.getRoles();
            allRoles.addAll(nuxeoRoles);
        }
        return allRoles;
    }

    /**
     * Loads Keycloak from configuration file
     *
     * @return the configuration file as an {@link InputStream}
     */
    private InputStream loadKeycloakConfigFile() {

        if (keycloakConfigFile.startsWith(PROTOCOL_CLASSPATH)) {
            String classPathLocation = keycloakConfigFile.replace(PROTOCOL_CLASSPATH, "");

            LOGGER.debug("Loading config from classpath on location: " + classPathLocation);

            // Try current class classloader first
            InputStream is = getClass().getClassLoader().getResourceAsStream(classPathLocation);
            if (is == null) {
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream(classPathLocation);
            }

            if (is != null) {
                return is;
            } else {
                String message = "Unable to find config from classpath: " + keycloakConfigFile;
                LOGGER.error(message);
                throw new RuntimeException(message);
            }
        } else {
            // Fallback to file
            try {
                LOGGER.debug("Loading config from file: " + keycloakConfigFile);
                return new FileInputStream(keycloakConfigFile);
            } catch (FileNotFoundException fnfe) {
                String message = "Config not found on " + keycloakConfigFile;
                LOGGER.error(message);
                throw new RuntimeException(message, fnfe);
            }
        }
    }

    public void setNuxeoUserService(NuxeoUserService nuxeoUserService) {
        this.nuxeoUserService = nuxeoUserService;
    }

    public void setKeycloakAuthenticatorProvider(KeycloakAuthenticatorProvider keycloakAuthenticatorProvider) {
        this.keycloakAuthenticatorProvider = keycloakAuthenticatorProvider;
    }
}
