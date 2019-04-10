/*
 * (C) Copyright 2006-2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.datadog.automation;

import org.nuxeo.datadog.reporter.DatadogReporterService;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;

@Operation(id = StartDatadog.ID, category = Constants.CAT_SERVICES, label = "Start datadog reporter", description = "Starts the Datadog reporter")
public class StartDatadog {

    protected static final String ID = "Datadog.Start";

    @Context
    private DatadogReporterService ddrs;

    @OperationMethod
    public void run() {
        ddrs.startReporter();
    }
}
