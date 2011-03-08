/*
 * (C) Copyright 2010-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Gagnavarslan ehf
 */
package org.nuxeo.ecm.ui.web.auth.digest;

import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.login.BaseLoginModule;
import org.nuxeo.runtime.api.Framework;

/**
 * Nuxeo Login Plugin for HTTP Digest Access Authentication (RFC 2617).
 */
public class DigestLoginPlugin extends BaseLoginModule {

    private static final Log log = LogFactory.getLog(DigestLoginPlugin.class);

    protected static final String REALM = "realm";

    protected static final String HTTP_METHOD = "httpMethod";

    protected static final String URI = "uri";

    protected static final String QOP = "qop";

    protected static final String NONCE = "nonce";

    protected static final String NC = "nc";

    protected static final String CNONCE = "cnonce";

    // TODO introspect this config
    protected static final String DIGEST_AUTH_DIRECTORY_NAME = "digest_auth";

    @Override
    public Boolean initLoginModule() {
        return Boolean.TRUE;
    }

    @Override
    public String validatedUserIdentity(UserIdentificationInfo userIdent) {
        try {
            String storedHA1 = getStoredHA1(userIdent.getUserName());

            if (StringUtils.isEmpty(storedHA1)) {
                log.warn("Digest authentication failed. Stored HA1 is empty");
                return null;
            }

            Map<String, String> loginParameters = userIdent.getLoginParameters();
            String generateDigest = generateDigest(storedHA1,
                    loginParameters.get(HTTP_METHOD), //
                    loginParameters.get(URI), //
                    loginParameters.get(QOP), // RFC 2617 extension
                    loginParameters.get(NONCE), //
                    loginParameters.get(NC), // RFC 2617 extension
                    loginParameters.get(CNONCE) // RFC 2617 extension
            );

            if (generateDigest.equals(userIdent.getPassword())) {
                return userIdent.getUserName();
            } else {
                log.warn("Digest authentication failed for user: "
                        + userIdent.getUserName() + " realm: "
                        + loginParameters.get(REALM));
                return null;
            }
        } catch (Exception e) {
            log.error("Digest authentication failed", e);
            return null;
        }
    }

    public static String generateDigest(String a1md5, String httpMethod,
            String uri, String qop, String nonce, String nc, String cnonce)
            throws IllegalArgumentException {
        String a2 = httpMethod + ":" + uri;
        String a2md5 = DigestUtils.md5Hex(a2);
        String digest;
        if (qop == null) {
            digest = a1md5 + ":" + nonce + ":" + a2md5;
        } else if ("auth".equals(qop)) {
            digest = a1md5 + ":" + nonce + ":" + nc + ":" + cnonce + ":" + qop
                    + ":" + a2md5;
        } else {
            throw new IllegalArgumentException(
                    "This method does not support a qop: '" + qop + "'");
        }
        return DigestUtils.md5Hex(digest);
    }

    public static String encodePassword(String username, String realm,
            String password) {
        String a1 = username + ":" + realm + ":" + password;
        return DigestUtils.md5Hex(a1);
    }

    protected String getStoredHA1(String username) throws Exception {
        DirectoryService directoryService = Framework.getService(DirectoryService.class);
        Directory directory = directoryService.getDirectory(DIGEST_AUTH_DIRECTORY_NAME);
        if (directory == null) {
            throw new IllegalArgumentException("Directory '"
                    + DIGEST_AUTH_DIRECTORY_NAME + "' was not found.");
        }
        Session dir = null;
        try {
            dir = directoryService.open(DIGEST_AUTH_DIRECTORY_NAME);
            String schema = directoryService.getDirectorySchema(DIGEST_AUTH_DIRECTORY_NAME);
            DocumentModel entry = dir.getEntry(username, true);
            if (entry == null) {
                return null;
            }
            return (String) entry.getProperty(schema, dir.getPasswordField());
        } finally {
            if (dir != null) {
                dir.close();
            }
        }
    }

}
