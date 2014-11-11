/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.operations;

import java.net.URL;

import org.nuxeo.ecm.automation.OperationContext;
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
 * @deprecated Not enabled for now since not fully implemented. To activate it
 *             uncomment the registration from
 *             {@link AutomationComponent#activate(org.nuxeo.runtime.model.ComponentContext)}
 *             and enable the unit test.
 */
@Deprecated
@Operation(id = RunScriptFile.ID, category = Constants.CAT_SCRIPTING, label = "Run Script File", description = "Run a script file in the current context. The file is located using the bundle class loader.")
public class RunScriptFile {

    public static final String ID = "Context.RunScriptFile";

    @Context
    protected OperationContext ctx;

    @Param(name = "script")
    protected URL script;

    @OperationMethod
    public void run() throws Exception {
        Scripting.run(ctx, script);
    }

}
