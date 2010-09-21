package org.nuxeo.ecm.core.management.test.storage;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.management.storage.DocumentStoreHandler;

public class FakeDocumentStoreHandler implements DocumentStoreHandler {

    String repositoryName;

    protected static FakeDocumentStoreHandler testInstance;

    public FakeDocumentStoreHandler() {
        testInstance = this;
    }
    @Override
    public void onStorageInitialization(CoreSession session) {
       repositoryName = session.getRepositoryName();
    }

}
