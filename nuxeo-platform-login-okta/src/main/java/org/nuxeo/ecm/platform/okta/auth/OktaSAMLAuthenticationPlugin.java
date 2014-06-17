/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.okta.auth;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPluginLogoutExtension;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.opensaml.ws.security.SecurityPolicyException;

import com.okta.saml.Application;
import com.okta.saml.Configuration;
import com.okta.saml.SAMLRequest;
import com.okta.saml.SAMLResponse;
import com.okta.saml.SAMLValidator;

public class OktaSAMLAuthenticationPlugin implements NuxeoAuthenticationPlugin,
        NuxeoAuthenticationPluginLogoutExtension {

    private static final Log log = LogFactory.getLog(OktaSAMLAuthenticationPlugin.class);

    protected Application oktaApp;

    protected SAMLValidator validator;

    protected Configuration configuration;

    protected static final String SAML_REQUEST = "SAMLRequest";

    protected static final String SAML_RESPONSE = "SAMLResponse";

    protected static final String RELAY_STATE = "RelayState";

    protected static final String OKTA_FILE_KEY = "OktaConfig";

    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }

    protected void initOktaAppFromConfig(Map<String, String> parameters)
            throws Exception {
        validator = new SAMLValidator();
        String configFileName = parameters.get(OKTA_FILE_KEY);
        InputStream stream = Framework.getResourceLoader().getResourceAsStream(
                configFileName);
        String oktaXmlConfig = IOUtils.toString(stream, "UTF-8");
        // Load configuration from the xml for the template app
        configuration = validator.getConfiguration(oktaXmlConfig);
        oktaApp = configuration.getDefaultApplication();
    }

    protected Application getOktaApp() {
        return oktaApp;
    }

    public void initPlugin(Map<String, String> parameters) {
        try {
            initOktaAppFromConfig(parameters);
        } catch (Exception e) {
            log.error("Error during Okta initialization", e);
        }
    }

    public Boolean handleLoginPrompt(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, String baseURL) {

        Application app = getOktaApp();

        SAMLRequest samlRequest = validator.getSAMLRequest(app);
        String encodedSaml = Base64.encodeBase64String(samlRequest.toString().getBytes());
        String loginURL = app.getAuthenticationURL();
        try {
            loginURL += "?" + SAML_REQUEST + "="
                    + URLEncoder.encode(encodedSaml, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            log.error("Error while encoding URL", e1);
            return false;
        }

        if (log.isDebugEnabled()) {
            log.debug("Send redirect to " + loginURL);
        }
        try {
            httpResponse.sendRedirect(loginURL);
        } catch (IOException e) {
            String errorMessage = String.format(
                    "Unable to send redirect on %s", loginURL);
            log.error(errorMessage, e);
            return false;
        }
        return true;
    }

    @Override
    public Boolean handleLogout(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        return true;
    }

    @Override
    public UserIdentificationInfo handleRetrieveIdentity(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        if (!"POST".equals(httpRequest.getMethod())) {
            return null;
        }

        String assertion = httpRequest.getParameter(SAML_RESPONSE);
        if (assertion == null) {
            if (log.isDebugEnabled()) {
                log.debug("Unable to find SAML RESPONSE");
            }
            return null;
        }

        try {
            assertion = new String(
                    Base64.decodeBase64(assertion.getBytes("UTF-8")),
                    Charset.forName("UTF-8"));
        } catch (Exception e) {
            log.error("Error during SAML decoding", e);
            return null;
        }

        if (log.isDebugEnabled()) {
            log.debug("Retrieved SAML assertion : " + assertion);
        }

        // validate against Okta IDM
        SAMLResponse samlResponse;
        try {
            samlResponse = validator.getSAMLResponse(assertion, configuration);
        } catch (SecurityPolicyException e1) {
            log.error("Unable to validate SAML response", e1);
            return null;
        }

        String userId = samlResponse.getUserID();
        if (userId == null || "".equals(userId)) {
            return null;
        }

        try {
            createOrUpdate(userId, null);
        } catch (Exception e) {
            log.error("Unable to create or update user", e);
        }

        return new UserIdentificationInfo(userId, userId);
    }

    protected DocumentModel createOrUpdate(String userId,
            Map<String, Object> attributes) throws Exception {
        UserManager userManager = Framework.getService(UserManager.class);

        Session userDir = Framework.getService(DirectoryService.class).open(
                userManager.getUserDirectoryName());
        DocumentModel entry = null;

        try {
            entry = userDir.getEntry(userId);
            if (entry == null) {
                // userDir.createEntry(fieldMap);
            } else {
                entry.getDataModel(userManager.getUserSchemaName()).setMap(
                        attributes);
                userDir.updateEntry(entry);
            }
            userDir.commit();
        } finally {
            userDir.close();
        }
        return entry;
    }

    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return true;
    }

}
