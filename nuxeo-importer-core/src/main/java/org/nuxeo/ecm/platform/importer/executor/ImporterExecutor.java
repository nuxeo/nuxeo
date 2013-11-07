package org.nuxeo.ecm.platform.importer.executor;

import org.nuxeo.ecm.platform.importer.base.ImporterRunner;
import org.nuxeo.ecm.platform.importer.factories.ImporterDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.threading.ImporterThreadingPolicy;

/**
 *
 * Interface for Executor main thread.
 *
 * The role of the implementation is to create and configure the correct ImporterRunner and run it.
 *
 * This indirection was created to allow easy JAX-RS bindings to do run, getStatus, getLogger ...
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
public interface ImporterExecutor {

    public abstract ImporterLogger getLogger();

    public abstract String getStatus();

    public abstract Boolean isRunning();

    public abstract String kill();

    public abstract ImporterThreadingPolicy getThreadPolicy();

    public abstract void setThreadPolicy(ImporterThreadingPolicy threadPolicy);

    public abstract ImporterDocumentModelFactory getFactory();

    public abstract void setFactory(ImporterDocumentModelFactory factory);

    /***
     * since 5.5 this method is invoked when using the
     * <code>DefaultImporterService</code> and passing the executor to the
     * importDocuments method
     *
     * @param runner
     * @param interactive
     * @return
     * @throws Exception
     */
    public abstract String run(ImporterRunner runner, Boolean interactive)
            throws Exception;

}