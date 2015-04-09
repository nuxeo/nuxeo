/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thierry Delprat <tdelprat@nuxeo.com>
 */
package org.nuxeo.automation.scripting.internals;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.util.DataModelProperties;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.runtime.api.Framework;

/**
 * Class injected/published in Nashorn engine to execute automation service.
 *
 * @since 7.2
 */
public class AutomationMapper {

    protected final CoreSession session;
    protected final OperationContext ctx;

    public AutomationMapper(CoreSession session) {
        this.session = session;
        ctx = new OperationContext(session);
    }

    public Object executeOperation(String opId, Object input, ScriptObjectMirror parameters) throws Exception {
        AutomationService automationService = Framework.getService(AutomationService.class);
        populateContext(ctx, input);
        Map<String, Object> params = unwrapParameters(parameters);
        return automationService.run(ctx, opId, params);
    }

    protected Map<String, Object> unwrapParameters(ScriptObjectMirror parameters) {
        Map<String, Object> params = new HashMap<String, Object>();
        for (String k : parameters.keySet()) {
            Object value = parameters.get(k);
            if (value instanceof ScriptObjectMirror) {
                ScriptObjectMirror jso = (ScriptObjectMirror) value;
                if (jso.isArray()) {
                    params.put(k, MarshalingHelper.unwrap(jso));
                } else {
                    params.put(k, extractProperties(jso));
                }
            } else {
                if (value != null) {
                    params.put(k, value.toString());
                } else {
                    params.put(k, null);
                }
            }
        }
        return params;
    }

    protected void populateContext(OperationContext ctx, Object input) {
        if (input instanceof String) {
            ctx.setInput(input);
        } else if (input instanceof DocumentModel) {
            ctx.setInput(input);
        } else if (input instanceof DocumentRef) {
            ctx.setInput(input);
        } else if (input instanceof Blob) {
            ctx.setInput(input);
        } else if (input instanceof ScriptObjectMirror) {
            ctx.setInput(extractProperties((ScriptObjectMirror) input));
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


}
