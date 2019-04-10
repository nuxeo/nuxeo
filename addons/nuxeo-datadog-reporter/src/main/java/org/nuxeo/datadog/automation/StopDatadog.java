/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.datadog.automation;

import org.nuxeo.datadog.reporter.DatadogReporterService;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;

@Operation(id = StopDatadog.ID, category = Constants.CAT_SERVICES, label = "Stop datadog reporter", description = "Stops the Datadog reporter")
public class StopDatadog {

    protected static final String ID = "Datadog.Stop";

    @Context
    private DatadogReporterService ddrs;

    @OperationMethod
    public void run() {
        ddrs.stopReporter();
    }
}
