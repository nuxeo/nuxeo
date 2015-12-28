/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
