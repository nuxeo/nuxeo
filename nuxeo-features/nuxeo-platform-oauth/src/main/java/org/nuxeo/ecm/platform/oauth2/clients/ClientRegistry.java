package org.nuxeo.ecm.platform.oauth2.clients;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */
public interface ClientRegistry {

    public static final String OAUTH2CLIENT_DIRECTORY_NAME = "oauth2Clients";

    public static final String OAUTH2CLIENT_SCHEMA = "oauth2Client";

    boolean hasClient(String clientId);

    boolean isValidClient(String clientId, String clientSecret);

    boolean registerClient(OAuth2Client client);

    boolean deleteClient(String clientId);

    List<DocumentModel> listClients();

    OAuth2Client getClient(String clientId);
}
