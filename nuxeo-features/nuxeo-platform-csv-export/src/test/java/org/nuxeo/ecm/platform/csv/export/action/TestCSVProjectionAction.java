/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     pierre
 */
package org.nuxeo.ecm.platform.csv.export.action;

import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.csv.export.action.CSVExportAction.ACTION_NAME;
import static org.nuxeo.ecm.platform.csv.export.computation.CSVProjectionComputation.PARAM_LANG;
import static org.nuxeo.ecm.platform.csv.export.computation.CSVProjectionComputation.PARAM_SCHEMAS;
import static org.nuxeo.ecm.platform.csv.export.computation.CSVProjectionComputation.PARAM_XPATHS;

import java.io.Serializable;
import java.time.Duration;
import java.util.Arrays;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DocumentSetRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.csv.export.computation.TestCSVProjectionComputation;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.3
 */
@Features(CoreFeature.class)
@RunWith(FeaturesRunner.class)
@RepositoryConfig(init = DocumentSetRepositoryInit.class)
@Deploy("org.nuxeo.ecm.platform.csv.export")
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib.xml")
public class TestCSVProjectionAction {

    @Inject
    public CoreSession session;

    @Inject
    public BulkService service;

    @Test
    public void computationShouldNeverBlockDueToParamValue() throws InterruptedException {
        service.submit(createBuilder().build());
        for (Serializable value : TestCSVProjectionComputation.getValues()) {
            BulkCommand.Builder builder = createBuilder();
            for (String param : Arrays.asList(PARAM_SCHEMAS, PARAM_XPATHS, PARAM_LANG)) {
                builder.param(param, value);
            }
            service.submit(builder.build());
        }
        assertTrue(service.await(Duration.ofSeconds(60)));
    }

    protected BulkCommand.Builder createBuilder() {
        DocumentModel model = session.getDocument(new PathRef("/default-domain/workspaces/test"));
        String nxql = String.format("SELECT * FROM Document where ecm:ancestorId='%s'", model.getId());
        return new BulkCommand.Builder(ACTION_NAME, nxql).repository(session.getRepositoryName())
                                                         .user(session.getPrincipal().getName());
    }

}
