package org.nuxeo.ecm.platform.oauth2.clients;

import static org.nuxeo.ecm.platform.oauth2.clients.ClientRegistry.OAUTH2CLIENT_SCHEMA;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */
@XObject("client")
public class OAuth2Client {

    @XNode("@name")
    protected String name;
    @XNode("@id")
    protected String id;
    @XNode("@secret")
    protected String secret;
    @XNode("@enabled")
    protected boolean enabled = true;

    public OAuth2Client() { }

    public OAuth2Client(String name, String id, String secret) {
        this.name = name;
        this.id = id;
        this.secret = secret;
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

    Map<String, Object> toMap() {
        Map<String, Object> doc = new HashMap<>();
        doc.put("clientId", id);
        doc.put("clientSecret", secret);
        doc.put("name", name);
        doc.put("enabled", enabled);
        return doc;
    }

    static OAuth2Client fromDocumentModel(DocumentModel doc) throws ClientException {
        String name = (String) doc.getPropertyValue(OAUTH2CLIENT_SCHEMA + ":name");
        String id = (String) doc.getPropertyValue(OAUTH2CLIENT_SCHEMA + ":clientId");
        String secret = (String) doc.getPropertyValue(OAUTH2CLIENT_SCHEMA + ":clientSecret");
        boolean enabled = (Boolean) doc.getPropertyValue(OAUTH2CLIENT_SCHEMA + ":enabled");

        OAuth2Client client = new OAuth2Client(name, id, secret);
        client.enabled = enabled;
        return client;
    }

    boolean isValidWith(String clientId, String clientSecret) {
        // Related to RFC 6749 2.3.1 clientSecret is omitted if empty
        return enabled && id.equals(clientId) && (StringUtils.isEmpty(secret) || secret.equals(clientSecret));
    }
}
