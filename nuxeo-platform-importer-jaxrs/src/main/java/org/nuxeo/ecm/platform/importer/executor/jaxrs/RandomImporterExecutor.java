package org.nuxeo.ecm.platform.importer.executor.jaxrs;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.importer.base.GenericMultiThreadedImporter;
import org.nuxeo.ecm.platform.importer.filter.EventServiceConfiguratorFilter;
import org.nuxeo.ecm.platform.importer.filter.ImporterFilter;
import org.nuxeo.ecm.platform.importer.source.RandomTextSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

@Path("randomImporter")
public class RandomImporterExecutor extends AbstractJaxRSImporterExecutor {

    private static final Log log = LogFactory.getLog(RandomImporterExecutor.class);

    @Override
    protected Log getJavaLogger() {
        return log;
    }

    @GET
    @Path("run")
    @Produces("text/plain; charset=UTF-8")
    public String run(
            @QueryParam("targetPath") String targetPath,
            @QueryParam("batchSize") Integer batchSize,
            @QueryParam("nbThreads") Integer nbTheards,
            @QueryParam("interactive") Boolean interactive,
            @QueryParam("nbNodes") Integer nbNodes,
            @QueryParam("fileSizeKB") Integer fileSizeKB,
            @QueryParam("onlyText") Boolean onlyText,
            @QueryParam("blockSyncPostCommitProcessing") Boolean blockSyncPostCommitProcessing,
            @QueryParam("blockAsyncProcessing") Boolean blockAsyncProcessing,
            @QueryParam("bulkMode") Boolean bulkMode) throws Exception {

        if (onlyText == null) {
            onlyText = true;
        }

        if (bulkMode == null) {
            bulkMode = true;
        }

        SourceNode source = null;
        getLogger().info("Init Random text generator");
        source = RandomTextSourceNode.init(nbNodes, fileSizeKB, onlyText);
        getLogger().info("Random text generator initialized");

        GenericMultiThreadedImporter runner = new GenericMultiThreadedImporter(
                source, targetPath, batchSize, nbTheards, getLogger());

        ImporterFilter filter = new EventServiceConfiguratorFilter(
                blockSyncPostCommitProcessing, blockAsyncProcessing, !onlyText,
                bulkMode);
        runner.addFilter(filter);

        String res = doRun(runner, interactive);
        return res;
    }

}
