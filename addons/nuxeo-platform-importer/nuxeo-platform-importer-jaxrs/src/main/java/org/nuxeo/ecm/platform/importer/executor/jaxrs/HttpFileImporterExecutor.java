package org.nuxeo.ecm.platform.importer.executor.jaxrs;

import java.io.File;

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
import org.nuxeo.ecm.platform.importer.source.FileWithMetadataSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

@Path("fileImporter")
public class HttpFileImporterExecutor extends AbstractJaxRSImporterExecutor {

    private static final Log log = LogFactory.getLog(HttpFileImporterExecutor.class);

    @Override
    protected Log getJavaLogger() {
        return log;
    }

    @GET
    @Path("run")
    @Produces("text/plain; charset=UTF-8")
    public String run(
            @QueryParam("inputPath") String inputPath,
            @QueryParam("targetPath") String targetPath,
            @QueryParam("skipRootContainerCreation") Boolean skipRootContainerCreation,
            @QueryParam("batchSize") Integer batchSize,
            @QueryParam("nbThreads") Integer nbThreads,
            @QueryParam("interactive") Boolean interactive) throws Exception {
        File srcFile = new File(inputPath);
        SourceNode source = new FileWithMetadataSourceNode(srcFile);

        ImporterRunnerConfiguration configuration = new ImporterRunnerConfiguration.Builder(
                source, targetPath, getLogger()).skipRootContainerCreation(
                skipRootContainerCreation).batchSize(batchSize).nbThreads(
                nbThreads).build();
        GenericMultiThreadedImporter runner = new GenericMultiThreadedImporter(
                configuration);

        ImporterFilter filter = new EventServiceConfiguratorFilter(false,
                false, false, true);
        runner.addFilter(filter);

        return doRun(runner, interactive);
    }

}
