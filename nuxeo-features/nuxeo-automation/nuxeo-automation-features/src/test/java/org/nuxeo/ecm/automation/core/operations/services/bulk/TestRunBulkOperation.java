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
package org.nuxeo.ecm.automation.core.operations.services.bulk;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.bulk.BulkStatus;
import org.nuxeo.ecm.core.bulk.DocumentSetRepositoryInit;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.2
 */
@Features(CoreFeature.class)
@RunWith(FeaturesRunner.class)
@Deploy("org.nuxeo.ecm.core.bulk")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.server")
@Deploy("org.nuxeo.ecm.automation.features")
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/bulk-contrib-tests.xml")
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib.xml")
@RepositoryConfig(init = DocumentSetRepositoryInit.class, cleanup = Granularity.CLASS)
public class TestRunBulkOperation {

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService service;

    @Test
    public void test() throws OperationException {

        DocumentModel model = session.getDocument(new PathRef("/default-domain/workspaces/test"));
        String nxsql = String.format("SELECT * from Document where ecm:parentId='%s'", model.getId());

        OperationContext ctx = new OperationContext(session);
        Map<String, String> params = new HashMap<>();
        params.put("operation", "count");
        params.put("query", nxsql);

        BulkStatus result = (BulkStatus) service.run(ctx, RunBulkOperation.ID, params);

        Assert.assertNotNull(result);

    }

}
