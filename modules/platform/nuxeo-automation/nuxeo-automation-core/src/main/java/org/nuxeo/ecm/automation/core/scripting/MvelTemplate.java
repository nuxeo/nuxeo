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
package org.nuxeo.ecm.automation.core.scripting;

import org.mvel2.compiler.BlankLiteral;
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

    @Override
    public Object eval(OperationContext ctx) {
        if (compiled == null) {
            compiled = TemplateCompiler.compileTemplate(expr);
        }
        Object obj = TemplateRuntime.execute(compiled, Scripting.initBindings(ctx));
        return obj == null || obj.getClass().isAssignableFrom(BlankLiteral.class) ? "" : obj.toString();
    }

    @Override
    public String getExpr() {
        return expr;
    }

}
