/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat <tdelprat@nuxeo.com>
 */
package org.nuxeo.automation.scripting.internals.operation;

import java.util.Map;

import javax.script.ScriptException;

import org.nuxeo.automation.scripting.api.AutomationScriptingService;
import org.nuxeo.automation.scripting.internals.ScriptOperationContext;
import org.nuxeo.automation.scripting.internals.WrapperHelper;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 7.2
 */
public class ScriptingOperationImpl {

    protected final ScriptOperationContext ctx;

    protected final Map<String, Object> args;

    protected final String source;

    public ScriptingOperationImpl(String source, ScriptOperationContext ctx, Map<String, Object> args)
            throws ScriptException {
        this.ctx = ctx;
        this.args = args;
        this.source = source;
    }

    public Object run(Object input) throws Exception {
        try {
            AutomationScriptingService scriptingService = Framework.getService(AutomationScriptingService.class);
            scriptingService.setOperationContext(ctx);
            ScriptingOperationInterface itf = scriptingService.getInterface(ScriptingOperationInterface.class, source,
                    ctx.getCoreSession());
            input = wrapArgsAndInput(input, args);
            return unwrapResult(itf.run(input, args));
        } catch (ScriptException e) {
            throw new OperationException(e);
        } finally {
            if (ctx.get(Constants.VAR_IS_CHAIN) != null && !(Boolean) ctx.get(Constants.VAR_IS_CHAIN)) {
                ctx.deferredDispose();
            }
        }
    }

    protected Object wrapArgsAndInput(Object input, Map<String, Object> args) {
        args.replaceAll((key, value) -> WrapperHelper.wrap(value, ctx.getCoreSession()));
        return WrapperHelper.wrap(input, ctx.getCoreSession());
    }

    protected Object unwrapResult(Object res) {
        // Unwrap Context
        ctx.replaceAll((key, value) -> WrapperHelper.unwrap(value));
        // Unwrap Result
        return WrapperHelper.unwrap(res);
    }

}
