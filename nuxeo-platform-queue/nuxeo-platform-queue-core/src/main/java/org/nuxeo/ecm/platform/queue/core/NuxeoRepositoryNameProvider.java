package org.nuxeo.ecm.platform.queue.core;

import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;

public class NuxeoRepositoryNameProvider {

    private  NuxeoRepositoryNameProvider() {
        throw new UnsupportedOperationException();
    }

    protected  static String getRepositoryName() {
        return Framework.getLocalService(RepositoryManager.class).getDefaultRepository().getName();
    }

}
