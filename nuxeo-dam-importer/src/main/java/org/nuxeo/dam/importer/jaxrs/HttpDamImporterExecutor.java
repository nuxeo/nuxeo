/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.dam.importer.jaxrs;

import java.io.File;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.dam.importer.filter.DamImporterFilter;
import org.nuxeo.ecm.platform.importer.executor.jaxrs.AbstractJaxRSImporterExecutor;
import org.nuxeo.ecm.platform.importer.factories.FileManagerDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.filter.EventServiceConfiguratorFilter;
import org.nuxeo.ecm.platform.importer.filter.ImporterFilter;
import org.nuxeo.ecm.platform.importer.source.FileWithMetadataSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

/**
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@Path("damImporter")
public class HttpDamImporterExecutor extends AbstractJaxRSImporterExecutor {

    private static final Log log = LogFactory.getLog(HttpDamImporterExecutor.class);

    public static final String TARGET_PATH = "/default-domain/import-root";

    @Override
    protected Log getJavaLogger() {
        return log;
    }

    @GET
    @Path("run")
    @Produces("text/plain; charset=UTF-8")
    public String run(@QueryParam("inputPath") String inputPath,
            @QueryParam("importFolderTitle") String importFolderName,
            @QueryParam("importSetName") String importSetName,
            @QueryParam("batchSize") Integer batchSize , @QueryParam("nbThreads") Integer nbThreads,
            @QueryParam("interactive") Boolean interactive) throws Exception {
        File srcFile = new File(inputPath);
        SourceNode source = new FileWithMetadataSourceNode(srcFile);
        DamMultiThreadedImporter runner = new DamMultiThreadedImporter(source,
                TARGET_PATH, importFolderName, importSetName, batchSize,
                nbThreads, getLogger());
        runner.setFactory(new FileManagerDocumentModelFactory());

        ImporterFilter filter = new EventServiceConfiguratorFilter(false,
                false, false, true);
        runner.addFilter(filter);
        runner.addFilter(new DamImporterFilter());

        return doRun(runner, interactive);
    }

}
