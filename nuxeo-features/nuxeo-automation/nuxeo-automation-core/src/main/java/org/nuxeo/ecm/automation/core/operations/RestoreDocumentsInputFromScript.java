/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License (LGPL)
 * version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * Contributors: Sun Seng David TAN <stan@nuxeo.com>
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
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * Run a script and return the result documents object of the script the output
 * of the operation
 *
 * @since 5.6
 */
@Operation(id = RestoreDocumentsInputFromScript.ID, category = Constants.CAT_EXECUTION, label = "Restore input documents from a script", description = "Run a script and return the result documents object of the script the output of the operation")
public class RestoreDocumentsInputFromScript {

    public static final String ID = "Context.RestoreDocumentsInputFromScript";

    @Context
    protected OperationContext ctx;

    @Param(name = "script", widget = Constants.W_MULTILINE_TEXT)
    protected String script;

    private volatile Expression expr;

    @OperationMethod
    public DocumentModelList run() throws Exception {
        if (expr == null) {
            String text = script.replaceAll("&lt;", "<");
            text = text.replaceAll("&gt;", ">");
            text = text.replaceAll("&amp;", "&");
            expr = Scripting.newExpression(text);
        }
        return (DocumentModelList) expr.eval(ctx);
    }

}
