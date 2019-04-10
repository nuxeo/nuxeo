package org.nuxeo.ecm.platform.importer.executor.jaxrs;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.importer.base.GenericMultiThreadedImporter;
import org.nuxeo.ecm.platform.importer.base.ImporterRunnerConfiguration;
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
            @QueryParam("skipRootContainerCreation") Boolean skipRootContainerCreation,
            @QueryParam("batchSize") Integer batchSize,
            @QueryParam("nbThreads") Integer nbThreads,
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

        getLogger().info("Init Random text generator");
        SourceNode source = RandomTextSourceNode.init(nbNodes, fileSizeKB, onlyText);
        getLogger().info("Random text generator initialized");

        ImporterRunnerConfiguration configuration = new ImporterRunnerConfiguration.Builder(
                source, targetPath, getLogger()).skipRootContainerCreation(
                skipRootContainerCreation).batchSize(batchSize).nbThreads(
                nbThreads).build();
        GenericMultiThreadedImporter runner = new GenericMultiThreadedImporter(
                configuration);

        ImporterFilter filter = new EventServiceConfiguratorFilter(
                blockSyncPostCommitProcessing, blockAsyncProcessing, !onlyText,
                bulkMode);
        runner.addFilter(filter);

        String res = doRun(runner, interactive);
        return res;
    }

}
