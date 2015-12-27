/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.operations;

import java.io.IOException;
import java.net.URL;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.AutomationComponent;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.scripting.Scripting;

/**
 * Save the session - TODO remove this?
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @deprecated Not enabled for now since not fully implemented. To activate it uncomment the registration from
 *             {@link AutomationComponent#activate(org.nuxeo.runtime.model.ComponentContext)} and enable the unit test.
 */
@Deprecated
@Operation(id = RunScriptFile.ID, category = Constants.CAT_SCRIPTING, label = "Run Script File", description = "Run a script file in the current context. The file is located using the bundle class loader.", aliases = { "Context.RunScriptFile" })
public class RunScriptFile {

    public static final String ID = "RunScriptFile";

    @Context
    protected OperationContext ctx;

    @Param(name = "script")
    protected URL script;

    @OperationMethod
    public void run() throws OperationException, IOException {
        Scripting.run(ctx, script);
    }

}
