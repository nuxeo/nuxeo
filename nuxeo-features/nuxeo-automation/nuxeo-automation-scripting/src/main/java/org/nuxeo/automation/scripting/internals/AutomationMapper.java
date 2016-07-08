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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.core.util.DataModelProperties;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
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
        if (inputOutput instanceof ScriptObjectMirror) {
            ctx.setInput(extractProperties((ScriptObjectMirror) inputOutput));
        } else if (inputOutput instanceof DocumentScriptingWrapper) {
            ctx.setInput(((DocumentScriptingWrapper) inputOutput).getDoc());
        } else if (inputOutput instanceof List<?>) {
            DocumentModelList docs = new DocumentModelListImpl();
            List<?> l = (List<?>) inputOutput;
            for (Object item : l) {
                if (item instanceof DocumentScriptingWrapper) {
                    docs.add(((DocumentScriptingWrapper) item).getDoc());
                }
            }
            if (docs.size() == l.size() && docs.size() > 0) {
                ctx.setInput(docs);
            }
        } else {
            ctx.setInput(inputOutput);
        }
        for (String entryId : ctx.keySet()) {
            Object entry = ctx.get(entryId);
            if (entry instanceof DocumentScriptingWrapper) {
                ctx.put(entryId, ((DocumentScriptingWrapper) entry).getDoc());
            } else if (entry instanceof List<?>) {
                DocumentModelList docs = new DocumentModelListImpl();
                List<?> l = (List<?>) entry;
                for (Object item : l) {
                    if (item instanceof DocumentScriptingWrapper) {
                        docs.add(((DocumentScriptingWrapper) item).getDoc());
                    }
                }
                if (docs.size() == l.size() && docs.size() > 0) {
                    ctx.put(entryId, docs);
                }
            }
        }
    }

    protected Properties extractProperties(ScriptObjectMirror parameters) {
        DataModelProperties props = new DataModelProperties();
        Map<String, Object> data = MarshalingHelper.unwrapMap(parameters);
        for (String k : data.keySet()) {
            props.getMap().put(k, (Serializable) data.get(k));
        }
        return props;
    }

    protected Object wrapContextAndOutput(Object output) {
        for (String entryId : ctx.keySet()) {
            Object entry = ctx.get(entryId);
            if (entry instanceof DocumentModel) {
                ctx.put(entryId, new DocumentScriptingWrapper(ctx.getCoreSession(), (DocumentModel) entry));
            }
            if (entry instanceof DocumentModelList) {
                List<DocumentScriptingWrapper> docs = new ArrayList<>();
                for (DocumentModel doc : (DocumentModelList) entry) {
                    docs.add(new DocumentScriptingWrapper(ctx.getCoreSession(), doc));
                }
                ctx.put(entryId, docs);
            }
        }
        if (output instanceof DocumentModel) {
            return new DocumentScriptingWrapper(ctx.getCoreSession(), (DocumentModel) output);
        } else if (output instanceof DocumentModelList) {
            List<DocumentScriptingWrapper> docs = new ArrayList<>();
            for (DocumentModel doc : (DocumentModelList) output) {
                docs.add(new DocumentScriptingWrapper(ctx.getCoreSession(), doc));
            }
            return docs;
        }
        return output;
    }

    protected Map<String, Object> unwrapParameters(ScriptObjectMirror parameters) {
        Map<String, Object> params = new HashMap<>();
        for (String k : parameters.keySet()) {
            Object value = parameters.get(k);
            if (value instanceof ScriptObjectMirror) {
                ScriptObjectMirror jso = (ScriptObjectMirror) value;
                if (jso.isArray()) {
                    params.put(k, MarshalingHelper.unwrap(jso));
                } else {
                    params.put(k, extractProperties(jso));
                }
            } else if (value instanceof DocumentScriptingWrapper) {
                params.put(k, ((DocumentScriptingWrapper) value).getDoc());
            } else if (value instanceof List<?>) {
                DocumentModelList docs = new DocumentModelListImpl();
                List<?> l = (List<?>) value;
                for (Object item : l) {
                    if (item instanceof DocumentScriptingWrapper) {
                        docs.add(((DocumentScriptingWrapper) item).getDoc());
                    }
                }
                if (docs.size() == l.size() && docs.size() > 0) {
                    params.put(k, docs);
                }
            } else {
                params.put(k, value);
            }
        }
        return params;
    }

}
