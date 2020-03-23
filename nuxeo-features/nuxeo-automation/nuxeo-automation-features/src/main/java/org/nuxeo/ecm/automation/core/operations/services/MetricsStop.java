/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.automation.core.operations.services;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.runtime.metrics.MetricsService;

/**
 * Simple operation to stop metrics reporters
 *
 * @since 11.1
 */
@Operation(id = MetricsStop.ID, category = Constants.CAT_SERVICES, label = "Metrics", addToStudio = false, description = "Stops Metrics rerporer.")
public class MetricsStop {

    protected static final String ID = "Metrics.Stop";

    @Context
    protected MetricsService service;

    @OperationMethod
    public void run() {
        service.stopReporters();
    }
}
