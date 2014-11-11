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

import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;
import org.nuxeo.ecm.automation.OperationContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class MvelTemplate implements Expression {

    private static final long serialVersionUID = 1L;

    protected transient volatile CompiledTemplate compiled;

    protected final String expr;

    public MvelTemplate(String expr) {
        this.expr = expr;
    }

    public Object eval(OperationContext ctx) throws Exception {
        if (compiled == null) {
            compiled = TemplateCompiler.compileTemplate(expr);
        }
        Object obj = TemplateRuntime.execute(compiled,
                Scripting.initBindings(ctx));
        return obj == null ? "" : obj.toString();
    }

}
