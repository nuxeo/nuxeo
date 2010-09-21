package org.nuxeo.ecm.core.management.test;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.management.storage.DocumentStoreHandler;

public class FakeDocumentStoreHandler implements DocumentStoreHandler {

    public String repositoryName;

    public static FakeDocumentStoreHandler testInstance;

    public FakeDocumentStoreHandler() {
        testInstance = this;
    }
    @Override
    public void onStorageInitialization(CoreSession session) {
       repositoryName = session.getRepositoryName();
    }

}
