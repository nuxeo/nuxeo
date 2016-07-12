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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.automation.scripting.internals;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.core.util.DataModelProperties;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

/**
 * Class injected/published in Nashorn engine to execute automation service.
 *
 * @since 7.2
 */
public class AutomationMapper {

    protected final CoreSession session;

    public final ScriptOperationContext ctx;

    public AutomationMapper(CoreSession session, ScriptOperationContext operationContext) {
        this.session = session;
        ctx = operationContext;
    }

    public Object executeOperation(String opId, Object input, ScriptObjectMirror parameters) throws Exception {
        AutomationService automationService = Framework.getService(AutomationService.class);
        unwrapContext(ctx, input);
        Map<String, Object> params = unwrapParameters(parameters);
        Object output = automationService.run(ctx, opId, params);
        return wrapContextAndOutput(output);
    }

    public void unwrapContext(ScriptOperationContext ctx, Object inputOutput) {
        Object result = unwrap(inputOutput);
        ctx.setInput(result);
        ctx.replaceAll((key, value) -> WrapperHelper.unwrap(value));
    }

    protected Map<String, Object> unwrapParameters(ScriptObjectMirror parameters) {
        Map<String, Object> params = new HashMap<>();
        for (String key : parameters.keySet()) {
            params.put(key, unwrap(parameters.get(key)));
        }
        return params;
    }

    private Object unwrap(Object inputOutput) {
        Object result = WrapperHelper.unwrap(inputOutput);
        if (result instanceof Map<?, ?>) {
            result = computeProperties((Map<?, ?>) result);
        }
        return result;
    }

    protected Object wrapContextAndOutput(Object output) {
        ctx.replaceAll((key, value) -> WrapperHelper.wrap(value, ctx.getCoreSession()));
        return WrapperHelper.wrap(output, ctx.getCoreSession());
    }

    protected Properties computeProperties(Map<?, ?> result) {
        DataModelProperties props = new DataModelProperties();
        for (Entry<?, ?> entry : result.entrySet()) {
            props.getMap().put(entry.getKey().toString(), (Serializable) entry.getValue());
        }
        return props;
    }

}
