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

import static org.nuxeo.ecm.platform.oauth2.clients.ClientRegistry.OAUTH2CLIENT_SCHEMA;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */
@XObject("client")
public class OAuth2Client {

    private static final Log log = LogFactory.getLog(OAuth2Client.class);

    protected static final Pattern LOCALHOST_PATTERN = Pattern.compile("http://localhost(:\\d+)?(/.*)?");

    @XNode("@name")
    protected String name;

    @XNode("@id")
    protected String id;

    @XNode("@secret")
    protected String secret;

    /**
     * @since 9.2
     */
    @XNode("@redirectURI")
    protected String redirectURI;

    @XNode("@enabled")
    protected boolean enabled = true;

    public OAuth2Client() {
    }

    /**
     * @deprecated since 9.2, use {@link #OAuth2Client(String, String, String, String)} instead
     */
    @Deprecated
    public OAuth2Client(String name, String id, String secret) {
        this(name, id, secret, null);
    }

    /**
     * @since 9.2
     */
    public OAuth2Client(String name, String id, String secret, String redirectURI) {
        this.name = name;
        this.id = id;
        this.secret = secret;
        this.redirectURI = redirectURI;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    /**
     * @since 9.2
     */
    public String getRedirectURI() {
        return redirectURI;
    }

    /**
     * @since 9.2
     */
    public void setRedirectURI(String redirectURI) {
        this.redirectURI = redirectURI;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enable) {
        this.enabled = enable;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> doc = new HashMap<>();
        doc.put("clientId", id);
        doc.put("clientSecret", secret);
        doc.put("redirectURI", redirectURI);
        doc.put("name", name);
        doc.put("enabled", enabled);
        return doc;
    }

    public static OAuth2Client fromDocumentModel(DocumentModel doc) {
        String name = (String) doc.getPropertyValue(OAUTH2CLIENT_SCHEMA + ":name");
        String id = (String) doc.getPropertyValue(OAUTH2CLIENT_SCHEMA + ":clientId");
        String secret = (String) doc.getPropertyValue(OAUTH2CLIENT_SCHEMA + ":clientSecret");
        String redirectURI = (String) doc.getPropertyValue(OAUTH2CLIENT_SCHEMA + ":redirectURI");
        boolean enabled = (Boolean) doc.getPropertyValue(OAUTH2CLIENT_SCHEMA + ":enabled");

        OAuth2Client client = new OAuth2Client(name, id, secret, redirectURI);
        client.enabled = enabled;
        return client;
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
        if (redirectURI.startsWith("https") || !redirectURI.startsWith("http")) {
            return true;
        }
        return LOCALHOST_PATTERN.matcher(redirectURI).matches();
    }

    /**
     * @since 9.2
     */
    public boolean isValid() {
        List<String> messages = new ArrayList<>();
        if (StringUtils.isBlank(id)) {
            messages.add("id is required");
        }
        if (StringUtils.isBlank(name)) {
            messages.add("name is required");
        }
        if (StringUtils.isBlank(redirectURI)) {
            messages.add("redirectURI is required");
        } else if (!isRedirectURIValid(redirectURI)) {
            messages.add("redirectURI must start with https for security reasons");
        }
        if (messages.isEmpty()) {
            return true;
        }
        log.error(String.format("Invalid OAuth2 client %s: %s.", this, String.join(", ", messages)));
        return false;
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
