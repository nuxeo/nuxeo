/*
 * (C) Copyright 2010-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Gagnavarslan ehf
 *     Thomas Haines
 *     Florent Guillaume
 */
package org.nuxeo.ecm.ui.web.auth.digest;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Nuxeo Authenticator for HTTP Digest Access Authentication (RFC 2617).
 */
public class DigestAuthenticator implements NuxeoAuthenticationPlugin {

    private static final Log log = LogFactory.getLog(DigestAuthenticator.class);

    protected static final String DEFAULT_REALMNAME = "NUXEO";

    protected static final long DEFAULT_NONCE_VALIDITY_SECONDS = 1000;

    protected static final String REALM = "realm";

    protected static final String HTTP_METHOD = "httpMethod";

    protected static final String URI = "uri";

    protected static final String QOP = "qop";

    protected static final String NONCE = "nonce";

    protected static final String NC = "nc";

    protected static final String CNONCE = "cnonce";

    protected static final String REALM_NAME_KEY = "RealmName";

    protected static final String BA_HEADER_NAME = "WWW-Authenticate";

    protected String realmName;

    protected long nonceValiditySeconds = DEFAULT_NONCE_VALIDITY_SECONDS;

    protected String accessKey = "key";

    @Override
    public Boolean handleLoginPrompt(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String baseURL) {

        long expiryTime = System.currentTimeMillis() + (nonceValiditySeconds * 1000);
        String signature = DigestUtils.md5Hex(expiryTime + ":" + accessKey);
        String nonce = expiryTime + ":" + signature;
        String nonceB64 = new String(Base64.encodeBase64(nonce.getBytes()));

        String authenticateHeader = String.format("Digest realm=\"%s\", qop=\"auth\", nonce=\"%s\"", realmName,
                nonceB64);

        try {
            httpResponse.addHeader(BA_HEADER_NAME, authenticateHeader);
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return Boolean.TRUE;
        } catch (IOException e) {
            return Boolean.FALSE;
        }
    }

    @Override
    public UserIdentificationInfo handleRetrieveIdentity(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String header = httpRequest.getHeader("Authorization");
        if (StringUtils.isEmpty(header)) {
            return null;
        }
        Map<String, String> headerMap = splitParameters(header);
        if (headerMap == null) {
            // parsing failed
            return null;
        }
        headerMap.put("httpMethod", httpRequest.getMethod());

        String nonceB64 = headerMap.get(NONCE);
        String nonce = new String(Base64.decodeBase64(nonceB64.getBytes()));
        String[] nonceTokens = nonce.split(":");
        @SuppressWarnings("unused")
        long nonceExpiryTime = Long.parseLong(nonceTokens[0]);
        // @TODO: check expiry time and do something

        String username = getValidatedUsername(headerMap);
        if (username == null) {
            return null; // invalid
        }
        return new UserIdentificationInfo(username);
    }

    @Override
    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        // @TODO: Use DIGEST authentication for WebDAV and WSS
        return Boolean.TRUE;
    }

    @Override
    public void initPlugin(Map<String, String> parameters) {
        if (parameters.containsKey(REALM_NAME_KEY)) {
            realmName = parameters.get(REALM_NAME_KEY);
        } else {
            realmName = DEFAULT_REALMNAME;
        }
    }

    @Override
    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }

    public static Map<String, String> splitParameters(String auth) {
        Map<String, String> map;
        try {
            map = org.apache.tomcat.util.http.parser.Authorization.parseAuthorizationDigest(new StringReader(auth));
        } catch (IllegalArgumentException | IOException e) {
            log.error(e.getMessage(), e);
            map = null;
        }
        return map;
    }

    protected String getValidatedUsername(Map<String, String> headerMap) {
        String username = headerMap.get("username");
        try {
            String storedHA1 = getStoredHA1(username);
            if (StringUtils.isEmpty(storedHA1)) {
                log.warn("Digest authentication failed, stored HA1 is empty for user: " + username);
                return null;
            }
            String computedDigest = computeDigest(storedHA1, //
                    headerMap.get(HTTP_METHOD), //
                    headerMap.get(URI), //
                    headerMap.get(QOP), // RFC 2617 extension
                    headerMap.get(NONCE), //
                    headerMap.get(NC), // RFC 2617 extension
                    headerMap.get(CNONCE) // RFC 2617 extension
            );
            String digest = headerMap.get("response");
            if (!computedDigest.equals(digest)) {
                log.warn("Digest authentication failed for user: " + username + ", realm: " + headerMap.get(REALM));
                return null;
            }
        } catch (IllegalArgumentException | DirectoryException e) {
            log.error("Digest authentication failed for user: " + username, e);
            return null;
        }
        return username;
    }

    protected static String computeDigest(String ha1, String httpMethod, String uri, String qop, String nonce,
            String nc, String cnonce) throws IllegalArgumentException {
        String a2 = httpMethod + ":" + uri;
        String ha2 = DigestUtils.md5Hex(a2);
        String digest;
        if (qop == null) {
            digest = ha1 + ":" + nonce + ":" + ha2;
        } else if ("auth".equals(qop)) {
            digest = ha1 + ":" + nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + ha2;
        } else {
            throw new IllegalArgumentException("This method does not support a qop: '" + qop + "'");
        }
        return DigestUtils.md5Hex(digest);
    }

    protected String getStoredHA1(String username) {
        UserManager userManager = Framework.getService(UserManager.class);
        String dirName = userManager.getDigestAuthDirectory();
        DirectoryService directoryService = Framework.getService(DirectoryService.class);
        Directory directory = directoryService.getDirectory(dirName);
        if (directory == null) {
            throw new IllegalArgumentException("Digest Auth directory not found: " + dirName);
        }
        try (Session dir = directoryService.open(dirName)) {
            dir.setReadAllColumns(true); // needed to read digest password
            String schema = directoryService.getDirectorySchema(dirName);
            DocumentModel entry = Framework.doPrivileged(() -> dir.getEntry(username, true));
            String passwordField = dir.getPasswordField();
            return entry == null ? null : (String) entry.getProperty(schema, passwordField);
        }
    }

}
