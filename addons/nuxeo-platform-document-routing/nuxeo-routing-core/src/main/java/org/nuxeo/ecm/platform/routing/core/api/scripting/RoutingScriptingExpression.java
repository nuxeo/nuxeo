/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Mariana Cedica
 */
package org.nuxeo.ecm.platform.routing.core.api.scripting;

import java.util.Map;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.scripting.MvelExpression;
import org.nuxeo.ecm.automation.core.scripting.Scripting;

/**
 * Overrides the MVELExpression to add specific helpers depending on the context
 *
 * @5.9.3
 */
public class RoutingScriptingExpression extends MvelExpression {

    private static final long serialVersionUID = 1L;

    protected RoutingScriptingFunctions fn;

    public RoutingScriptingExpression(String expr) {
        super(expr);
    }

    public RoutingScriptingExpression(String expr, RoutingScriptingFunctions fn) {
        super(expr);
        this.fn = fn;
    }

    @Override
    protected Map<String, Object> getBindings(OperationContext ctx) {
        Map<String, Object> bindings = Scripting.initBindings(ctx);
        if (fn != null) {
            bindings.put(RoutingScriptingFunctions.BINDING_KEY, fn);
        }
        return bindings;
    }
}
