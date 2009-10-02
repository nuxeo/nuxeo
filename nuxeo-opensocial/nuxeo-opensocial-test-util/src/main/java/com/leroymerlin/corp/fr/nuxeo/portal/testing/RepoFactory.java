package com.leroymerlin.corp.fr.nuxeo.portal.testing;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;

public interface RepoFactory {

    void createRepo(CoreSession session) throws ClientException;

}
