/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Arnaud Kervern
 */
package org.nuxeo.ecm.platform.oauth2.clients;

import static org.nuxeo.ecm.platform.oauth2.clients.OAuth2ClientService.OAUTH2CLIENT_SCHEMA;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */
public class OAuth2Client {

    protected static final Pattern LOCALHOST_PATTERN = Pattern.compile("http://localhost(:\\d+)?(/.*)?");

    protected String name;

    protected String id;

    protected String secret;

    /**
     * @since 9.2
     */
    protected List<String> redirectURIs;

    /**
     * @since 9.10
     */
    protected boolean autoGrant;

    protected boolean enabled;

    /**
     * @since 9.10
     */
    protected OAuth2Client(String name, String id, String secret, List<String> redirectURIs, boolean autoGrant,
            boolean enabled) {
        this.name = name;
        this.id = id;
        this.secret = secret;
        this.redirectURIs = redirectURIs;
        this.autoGrant = autoGrant;
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    /**
     * @since 9.2
     */
    public List<String> getRedirectURIs() {
        return redirectURIs;
    }

    /**
     * @since 9.10
     */
    public boolean isAutoGrant() {
        return autoGrant;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public static OAuth2Client fromDocumentModel(DocumentModel doc) {
        String name = (String) doc.getPropertyValue(OAUTH2CLIENT_SCHEMA + ":name");
        String id = (String) doc.getPropertyValue(OAUTH2CLIENT_SCHEMA + ":clientId");
        String secret = (String) doc.getPropertyValue(OAUTH2CLIENT_SCHEMA + ":clientSecret");
        List<String> redirectURIs;
        String redirectURIsProperty = (String) doc.getPropertyValue(OAUTH2CLIENT_SCHEMA + ":redirectURIs");
        if (StringUtils.isEmpty(redirectURIsProperty)) {
            redirectURIs = Collections.emptyList();
        } else {
            redirectURIs = Arrays.asList(redirectURIsProperty.split(","));
        }
        boolean autoGrant = (Boolean) Optional.ofNullable(doc.getPropertyValue(OAUTH2CLIENT_SCHEMA + ":autoGrant"))
                                              .orElse(false);
        boolean enabled = (Boolean) doc.getPropertyValue(OAUTH2CLIENT_SCHEMA + ":enabled");

        return new OAuth2Client(name, id, secret, redirectURIs, autoGrant, enabled);
    }

    /**
     * A redirect URI is considered as valid if and only if:
     * <ul>
     * <li>It is not empty</li>
     * <li>It starts with https, e.g. https://my.redirect.uri</li>
     * <li>It doesn't start with http, e.g. nuxeo://authorize</li>
     * <li>It starts with http://localhost with localhost not part of the domain name, e.g. http://localhost:8080/nuxeo,
     * a counter-example being http://localhost.somecompany.com</li>
     * </ul>
     *
     * @since 9.2
     */
    public static boolean isRedirectURIValid(String redirectURI) {
        String trimmed = redirectURI.trim();
        return !trimmed.isEmpty() && (trimmed.startsWith("https") || !trimmed.startsWith("http")
                || LOCALHOST_PATTERN.matcher(trimmed).matches());
    }

    public boolean isValidWith(String clientId, String clientSecret) {
        // Related to RFC 6749 2.3.1 clientSecret is omitted if empty
        return enabled && id.equals(clientId) && (StringUtils.isEmpty(secret) || secret.equals(clientSecret));
    }

    /**
     * @since 9.2
     */
    @Override
    public String toString() {
        return String.format("%s(name=%s, id=%s, redirectURIs=%s, autoGrant=%b, enabled=%b)",
                getClass().getSimpleName(), name, id, redirectURIs, autoGrant, enabled);
    }
}
