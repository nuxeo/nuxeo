/*
 * (C) Copyright 2010-2018 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.ui.web.auth.digest;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;

/**
 * Nuxeo Authenticator for HTTP Digest Access Authentication (RFC 2617).
 */
public class DigestAuthenticator implements NuxeoAuthenticationPlugin {

    private static final Log log = LogFactory.getLog(DigestAuthenticator.class);

    protected static final String DEFAULT_REALMNAME = "NUXEO";

    protected static final long DEFAULT_NONCE_VALIDITY_SECONDS = 1000;

    protected static final String EQUAL_SEPARATOR = "=";

    protected static final String QUOTE = "\"";

    /*
     * match the first portion up until an equals sign followed by optional white space of quote chars and ending with
     * an optional quote char Pattern is a thread-safe class and so can be defined statically Example pair pattern:
     * username="kirsty"
     */
    protected static final Pattern PAIR_ITEM_PATTERN = Pattern.compile("^(.*?)=([\\s\"]*)?(.*)(\")?$");

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
        String DIGEST_PREFIX = "digest ";
        if (StringUtils.isEmpty(header) || !header.toLowerCase().startsWith(DIGEST_PREFIX)) {
            return null;
        }
        Map<String, String> headerMap = splitParameters(header.substring(DIGEST_PREFIX.length()));
        headerMap.put("httpMethod", httpRequest.getMethod());

        String nonceB64 = headerMap.get("nonce");
        String nonce = new String(Base64.decodeBase64(nonceB64.getBytes()));
        String[] nonceTokens = nonce.split(":");

        @SuppressWarnings("unused")
        long nonceExpiryTime = Long.parseLong(nonceTokens[0]);
        // @TODO: check expiry time and do something

        String username = headerMap.get("username");
        String responseDigest = headerMap.get("response");
        UserIdentificationInfo userIdent = new UserIdentificationInfo(username, responseDigest);

        /*
         * I have used this property to transfer response parameters to DigestLoginPlugin But loginParameters rewritten
         * in NuxeoAuthenticationFilter common implementation
         * @TODO: Fix this or find new way to transfer properties to LoginPlugin
         */
        userIdent.setLoginParameters(headerMap);
        return userIdent;

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
        Map<String, String> map = new HashMap<>();
        try (CSVParser reader = new CSVParser(new StringReader(auth), CSVFormat.DEFAULT)) {
            Iterator<CSVRecord> iterator = reader.iterator();
            if (iterator.hasNext()) {
                CSVRecord record = iterator.next();
                for (String itemPairStr : record) {
                    itemPairStr = StringUtils.remove(itemPairStr, QUOTE);
                    String[] parts = itemPairStr.split(EQUAL_SEPARATOR, 2);
                    map.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return map;
    }

}
