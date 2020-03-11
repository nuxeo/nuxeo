/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.elasticsearch.operations.test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.elasticsearch.ElasticsearchWaitForIndexingOperation;
import org.nuxeo.runtime.api.Framework;

/**
 * Waits for Elasticsearch audit completion.
 *
 * @since 7.3
 */
@Operation(id = NuxeoDriveWaitForElasticsearchCompletion.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Wait for Elasticsearch audit completion")
public class NuxeoDriveWaitForElasticsearchCompletion {

    public static final String ID = "NuxeoDrive.WaitForElasticsearchCompletion";

    @Context
    protected OperationContext ctx;

    @OperationMethod
    public void run() throws OperationException {
        Map<String, Serializable> params = new HashMap<>();
        params.put("refresh", true);
        params.put("waitForAudit", true);
        Framework.getService(AutomationService.class).run(ctx, ElasticsearchWaitForIndexingOperation.ID, params);
    }

}
