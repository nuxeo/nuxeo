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

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.Map;

import javax.script.ScriptException;

import org.nuxeo.automation.scripting.api.AutomationScriptingService;
import org.nuxeo.automation.scripting.api.AutomationScriptingService.Session;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 7.2
 */
public class ScriptingOperationImpl {

    protected final String script;

    protected final OperationContext ctx;

    protected final Map<String, Object> args;

    public ScriptingOperationImpl(String script, OperationContext ctx, Map<String, Object> args) {
        this.script = script;
        this.ctx = ctx;
        this.args = args;
    }

    public interface Runnable {
        Object run(Object input, Map<String, Object> parameters);
    };

    public Object run() throws Exception {
        try (Session session = Framework.getService(AutomationScriptingService.class).get(ctx)) {
            return session.handleof(new ByteArrayInputStream(script.getBytes(Charset.forName("UTF-8"))), Runnable.class).run(ctx.getInput(), args);
        } catch (ScriptException e) {
            throw new OperationException(e);
        }
    }

}
