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

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static org.nuxeo.ecm.platform.oauth2.clients.OAuth2ClientService.OAUTH2CLIENT_SCHEMA;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */
public class OAuth2Client {

    /**
     * @since 11.1
     */
    public static final String NAME_FIELD = "name";

    /**
     * @since 11.1
     */
    public static final String ID_FIELD = "clientId";

    /**
     * @since 11.1
     */
    public static final String SECRET_FIELD = "clientSecret";

    /**
     * @since 11.1
     */
    public static final String REDIRECT_URI_FIELD = "redirectURIs";

    /**
     * @since 11.1
     */
    public static final String AUTO_GRANT_FIELD = "autoGrant";

    /**
     * @since 11.1
     */
    public static final String ENABLED_FIELD = "enabled";

    /**
     * @since 11.1
     */
    public static final String REDIRECT_URI_SEPARATOR = ",";

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

    /**
     * @since 11.1
     */
    public String getSecret() {
        return secret;
    }

    public static OAuth2Client fromDocumentModel(DocumentModel doc) {
        String name = (String) doc.getProperty(OAUTH2CLIENT_SCHEMA, NAME_FIELD);
        String id = (String) doc.getProperty(OAUTH2CLIENT_SCHEMA, ID_FIELD);
        boolean autoGrant = requireNonNullElse((Boolean) doc.getProperty(OAUTH2CLIENT_SCHEMA, AUTO_GRANT_FIELD), false);
        boolean enabled = requireNonNullElse((Boolean) doc.getProperty(OAUTH2CLIENT_SCHEMA, ENABLED_FIELD), false);
        String redirectURIsProperty = requireNonNullElse(
                (String) doc.getProperty(OAUTH2CLIENT_SCHEMA, REDIRECT_URI_FIELD), StringUtils.EMPTY);
        List<String> redirectURIs = Arrays.asList(StringUtils.split(redirectURIsProperty, REDIRECT_URI_SEPARATOR));
        String secret = (String) doc.getProperty(OAUTH2CLIENT_SCHEMA, SECRET_FIELD);

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
     * <li>The Nuxeo node is in Dev mode</li>
     * </ul>
     *
     * @since 9.2
     */
    public static boolean isRedirectURIValid(String redirectURI) {
        String trimmed = redirectURI.trim();
        return !trimmed.isEmpty() && (trimmed.startsWith("https") || !trimmed.startsWith("http")
                || LOCALHOST_PATTERN.matcher(trimmed).matches() || Framework.isDevModeSet());
    }

    public boolean isValidWith(String clientId, String clientSecret) {
        // Related to RFC 6749 2.3.1 clientSecret is omitted if empty
        return enabled && id.equals(clientId) && (StringUtils.isEmpty(secret) || secret.equals(clientSecret));
    }

    /**
     * Creates a {@link DocumentModel} from an {@link OAuth2Client}.
     *
     * @param oAuth2Client the {@code OAuth2Client} to convert
     * @return the {@code DocumentModel} corresponding to the {@code OAuth2Client}
     * @since 11.1
     */
    public static DocumentModel fromOAuth2Client(OAuth2Client oAuth2Client) {
        return BaseSession.createEntryModel(null, OAUTH2CLIENT_SCHEMA, null, toMap(oAuth2Client));
    }

    /**
     * Updates the {@link DocumentModel} by the {@link OAuth2Client}.
     *
     * @param documentModel the document model to update
     * @param oAuth2Client the new values of document
     * @return the updated {@code DocumentModel}
     * @throws NullPointerException if the documentModel or oAuth2Client is {@code null}
     * @since 11.1
     */
    public static DocumentModel updateDocument(DocumentModel documentModel, OAuth2Client oAuth2Client) {
        requireNonNull(documentModel, "documentModel model is required");
        documentModel.setProperties(OAUTH2CLIENT_SCHEMA, OAuth2Client.toMap(oAuth2Client));
        return documentModel;
    }

    /**
     * Converts an {@link OAuth2Client} to map structure.
     *
     * @param oAuth2Client the {@code OAuth2Client}
     * @return a map representing the {@code OAuth2Client}
     * @since 11.1
     */
    public static Map<String, Object> toMap(OAuth2Client oAuth2Client) {
        Map<String, Object> values = new HashMap<>();
        values.put(NAME_FIELD, oAuth2Client.getName());
        values.put(ID_FIELD, oAuth2Client.getId());
        values.put(REDIRECT_URI_FIELD, StringUtils.join(oAuth2Client.getRedirectURIs(), REDIRECT_URI_SEPARATOR));
        values.put(AUTO_GRANT_FIELD, oAuth2Client.isAutoGrant());
        values.put(ENABLED_FIELD, oAuth2Client.isEnabled());
        if (StringUtils.isNotEmpty(oAuth2Client.getSecret())) {
            values.put(SECRET_FIELD, oAuth2Client.getSecret());
        }
        return values;
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
