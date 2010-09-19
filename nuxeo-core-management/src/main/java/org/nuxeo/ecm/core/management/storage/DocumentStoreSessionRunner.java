package org.nuxeo.ecm.core.management.storage;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.management.api.StorageError;
import org.nuxeo.runtime.api.Framework;

public abstract class DocumentStoreSessionRunner extends UnrestrictedSessionRunner {

    protected static String repositoryName;

    /**
     *  Invoked by the core management component at startup
     *
     * @param name
     */
    public void setRepositoryName(String name) {
        repositoryName = name;
    }

    /**
     * TODO remove lazy logic once the contribution will be in place
     * @return
     */
    protected static String repositoryName() {
         if (repositoryName != null) {
             return repositoryName;
         }
         synchronized (DocumentStoreSessionRunner.class) {
             if (repositoryName != null) {
                 return repositoryName;
             }
             return repositoryName = Framework.getLocalService(RepositoryManager.class).getDefaultRepository().getName();
         }
    }

    public DocumentStoreSessionRunner() {
        super(repositoryName());
    }

    public void runProtected() {
        try {
            super.runUnrestricted();
        } catch (ClientException e) {
            throw new StorageError("Storage error :  " + errorMessage(), e);
        }
    }

    protected void runInNuxeoCL() {
        ClassLoader jarCL = Thread.currentThread().getContextClassLoader();
        ClassLoader bundleCL = Framework.class.getClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(bundleCL);
            runProtected();
        } finally {
            Thread.currentThread().setContextClassLoader(jarCL);
        }
    }

    protected String errorMessage() {
        return String.format("%s:%s", getClass().getCanonicalName(),this.toString());
    }

}
