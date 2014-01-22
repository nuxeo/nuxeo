package org.nuxeo.ecm.platform.oauth2.clients;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * OAuth2 Client registry component
 *
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */
public class ClientRegistryImpl extends DefaultComponent implements ClientRegistry{

    private static final Log log = LogFactory.getLog(ClientRegistry.class);

    @Override
    public boolean hasClient(String clientId) throws ClientException {
        DirectoryService service = getService();
        Session session = null;
        try {
            session = service.open(OAUTH2CLIENT_DIRECTORY_NAME);
            return session.hasEntry(clientId);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @Override
    public boolean isValidClient(String clientId, String clientSecret) throws ClientException {
        DirectoryService service = getService();
        Session session = null;
        try {
            session = service.open(OAUTH2CLIENT_DIRECTORY_NAME);
            DocumentModel docClient = session.getEntry(clientId);

            if (docClient != null) {
                OAuth2Client client = OAuth2Client.fromDocumentModel(docClient);
                return client.isValidWith(clientId, clientSecret);
            }
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return false;
    }

    @Override
    public boolean registerClient(OAuth2Client client) throws ClientException {
        DirectoryService service = getService();
        Session session = null;
        try {
            session = service.open(OAUTH2CLIENT_DIRECTORY_NAME);
            if (session.hasEntry(client.getId())) {
                log.debug(String.format("ClientId is already registered: %s", client.getId()));
                return false;
            }

            session.createEntry(client.toMap());
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return true;
    }

    @Override
    public boolean deleteClient(String clientId) throws ClientException {
        DirectoryService service = getService();
        Session session = null;
        try {
            session = service.open(OAUTH2CLIENT_DIRECTORY_NAME);
            session.deleteEntry(clientId);
            return true;
        } catch (DirectoryException e) {
            return false;
        }
        finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @Override
    public List<DocumentModel> listClients() throws ClientException {
        DirectoryService service = getService();
        Session session = null;
        try {
            session = service.open(OAUTH2CLIENT_DIRECTORY_NAME);
            return session.getEntries();
        }
        finally {
            if (session != null) {
                session.close();
            }
        }
    }

    protected DirectoryService getService() {
        return Framework.getLocalService(DirectoryService.class);
    }
}
