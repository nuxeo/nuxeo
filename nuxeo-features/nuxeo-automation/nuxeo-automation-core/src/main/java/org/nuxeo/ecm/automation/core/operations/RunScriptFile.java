/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
