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

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.scripting.Expression;
import org.nuxeo.ecm.automation.core.scripting.Scripting;

/**
 * Save the session - TODO remove this?
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = RunScript.ID, category = Constants.CAT_SCRIPTING, label = "Run Script", description = "Run a script which content is specified as text in the 'script' parameter")
public class RunScript {

    public static final String ID = "Context.RunScript";

    @Context
    protected OperationContext ctx;

    @Param(name = "script", widget = Constants.W_MULTILINE_TEXT)
    protected String script;

    private volatile Expression expr;

    @OperationMethod
    public void run() throws Exception {
        if (expr == null) {
            String text = script.replaceAll("&lt;", "<");
            text = text.replaceAll("&gt;", ">");
            expr = Scripting.newExpression(text);
        }
        expr.eval(ctx);
    }

}
