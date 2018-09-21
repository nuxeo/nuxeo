/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.wopi;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.nuxeo.wopi.Constants.JWT_TOKEN_TTL;

import java.nio.charset.Charset;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.jwt.JWTClaims;
import org.nuxeo.ecm.jwt.JWTService;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 10.3
 */
public class Helpers {

    private static final Log log = LogFactory.getLog(Helpers.class);

    public static final Charset UTF_7 = new com.beetstra.jutf7.CharsetProvider().charsetForName("UTF-7");

    private Helpers() {
        // helper class
    }

    public static String readUTF7String(String s) {
        byte[] bytes = s.getBytes(UTF_8);
        return new String(bytes, UTF_7);
    }

    public static String createJWTToken() {
        return Framework.getService(JWTService.class).newBuilder().withTTL(JWT_TOKEN_TTL).build();
    }

    public static long getJWTTokenExp(String token) {
        Map<String, Object> claims = Framework.getService(JWTService.class).verifyToken(token);
        long expireAt = (long) claims.get(JWTClaims.CLAIM_EXPIRES_AT);
        return expireAt * 1000; // milliseconds
    }

    // copied from org.nuxeo.ecm.platform.ui.web.tag.fn.Functions which lives in nuxeo-platform-ui-web
    public static String principalFullName(NuxeoPrincipal principal) {
        String first = principal.getFirstName();
        String last = principal.getLastName();
        return userDisplayName(principal.getName(), first, last);
    }

    public static Blob getEditableBlob(DocumentModel doc, String xpath) {
        Blob blob = null;
        try {
            blob = (Blob) doc.getPropertyValue(xpath);
        } catch (PropertyException e) {
            // prevent server error
        }
        if (blob == null) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Cannot find blob with xpath=%s for document %s/%s.", xpath,
                        doc.getRepositoryName(), doc.getId()));
            }
            return null;
        }
        // ignore external blob providers
        BlobManager blobManager = Framework.getService(BlobManager.class);
        BlobProvider blobProvider = blobManager.getBlobProvider(blob);
        if (blobProvider != null && (!blobProvider.supportsUserUpdate() || blobProvider.getBinaryManager() == null)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Ignoring blob with xpath=%s for document %s/%s as is is backed by a BlobProvider preventing updates.",
                        xpath, doc.getRepositoryName(), doc.getId()));
            }
            return null;
        }
        return blob;
    }

    protected static String userDisplayName(String id, String first, String last) {
        if (StringUtils.isEmpty(first)) {
            if (StringUtils.isEmpty(last)) {
                return id;
            } else {
                return last;
            }
        } else {
            if (StringUtils.isEmpty(last)) {
                return first;
            } else {
                return first + ' ' + last;
            }
        }
    }

}
