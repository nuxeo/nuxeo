package org.nuxeo.ecm.platform.importer.executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.importer.base.GenericMultiThreadedImporter;
import org.nuxeo.ecm.platform.importer.source.FileSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

public class DefaultImporterExecutor extends AbstractImporterExecutor {

    private static final Log log = LogFactory.getLog(DefaultImporterExecutor.class);

    protected CoreSession session = null;

    protected GenericMultiThreadedImporter importer = null;

    public DefaultImporterExecutor(CoreSession session) {
        this.session = session;
    }

    @Override
    protected CoreSession getCoreSession() {
        return session;
    }

    @Override
    protected Log getJavaLogger() {
        return log;
    }

    public long getCreatedDocsCounter() {
        return importer.getCreatedDocsCounter();
    }

    public String run(String inputPath,String targetPath,Integer batchSize ,Integer nbTheards, Boolean interactive) throws Exception {
        SourceNode source = new FileSourceNode(inputPath);
        return run(source, targetPath, batchSize, nbTheards, interactive);
    }

    public String run(SourceNode source,String targetPath,Integer batchSize ,Integer nbTheards, Boolean interactive) throws Exception {
        importer = new GenericMultiThreadedImporter(source, targetPath, batchSize, nbTheards, getLogger());
        importer.setFactory(getFactory());
        importer.setThreadPolicy(getThreadPolicy());
        return doRun(importer, interactive);
    }

}

