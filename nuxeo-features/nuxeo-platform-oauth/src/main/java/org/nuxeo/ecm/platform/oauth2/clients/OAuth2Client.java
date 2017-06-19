/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
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
    protected String redirectURI;

    protected boolean enabled;

    /**
     * @since 9.2
     */
    protected OAuth2Client(String name, String id, String secret, String redirectURI, boolean enabled) {
        this.name = name;
        this.id = id;
        this.secret = secret;
        this.redirectURI = redirectURI;
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    /**
     * @since 9.2
     */
    public String getRedirectURI() {
        return redirectURI;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public static OAuth2Client fromDocumentModel(DocumentModel doc) {
        String name = (String) doc.getPropertyValue(OAUTH2CLIENT_SCHEMA + ":name");
        String id = (String) doc.getPropertyValue(OAUTH2CLIENT_SCHEMA + ":clientId");
        String secret = (String) doc.getPropertyValue(OAUTH2CLIENT_SCHEMA + ":clientSecret");
        String redirectURI = (String) doc.getPropertyValue(OAUTH2CLIENT_SCHEMA + ":redirectURI");
        boolean enabled = (Boolean) doc.getPropertyValue(OAUTH2CLIENT_SCHEMA + ":enabled");

        return new OAuth2Client(name, id, secret, redirectURI, enabled);
    }

    /**
     * A redirect URI is considered as valid if and only if:
     * <ul>
     * <li>It starts with https, e.g. https://my.redirect.uri</li>
     * <li>It doesn't start with http, e.g. nuxeo://authorize</li>
     * <li>It starts with http://localhost with localhost not part of the domain name, e.g. http://localhost:8080/nuxeo,
     * a counter-example being http://localhost.somecompany.com</li>
     * </ul>
     *
     * @since 9.2
     */
    public static boolean isRedirectURIValid(String redirectURI) {
        return redirectURI.startsWith("https") || !redirectURI.startsWith("http")
                || LOCALHOST_PATTERN.matcher(redirectURI).matches();
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
        return String.format("%s(name=%s, id=%s, redirectURI=%s, enabled=%b)", getClass().getSimpleName(), name, id,
                redirectURI, enabled);
    }
}
