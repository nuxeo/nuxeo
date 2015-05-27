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
package org.nuxeo.ecm.automation.core.scripting;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.mvel2.MVEL;
import org.mvel2.compiler.BlankLiteral;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.trace.TracerFactory;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class MvelExpression implements Expression {

    private static final long serialVersionUID = 1L;

    protected transient volatile Serializable compiled;

    protected final String expr;

    public MvelExpression(String expr) {
        this.expr = expr;
    }

    @Override
    public Object eval(OperationContext ctx) {
        if (compiled == null) {
            compiled = MVEL.compileExpression(expr);
        }
        Object result;

        // Get and/or Store mvel results in operation context in case when trace mode enabled
        TracerFactory factory = Framework.getService(TracerFactory.class);
        if (factory.getRecordingState()) {
            HashMap<String, Object> mvelResults = (HashMap<String, Object>) ctx.get(Constants.MVEL_RESULTS);
            if (mvelResults == null) {
                mvelResults = new HashMap<>();
            }
            if (mvelResults.get(expr) != null) {
                result = mvelResults.get(expr);
            } else {
                result = MVEL.executeExpression(compiled, getBindings(ctx));
                mvelResults.put(expr, result);
                ctx.put(Constants.MVEL_RESULTS, mvelResults);
            }
        } else {
            result = MVEL.executeExpression(compiled, getBindings(ctx));
        }

        return result != null && result.getClass().isAssignableFrom(BlankLiteral.class) ? "" : result;
    }

    /**
     * @since 5.9.3
     */
    protected Map<String, Object> getBindings(OperationContext ctx) {
        return Scripting.initBindings(ctx);
    }
}