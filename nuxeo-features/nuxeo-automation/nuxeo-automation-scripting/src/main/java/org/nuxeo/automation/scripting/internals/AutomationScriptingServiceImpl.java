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
 */
package org.nuxeo.automation.scripting.internals;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

import org.apache.commons.io.IOUtils;
import org.nuxeo.automation.scripting.api.AutomationScriptingConstants;
import org.nuxeo.automation.scripting.api.AutomationScriptingException;
import org.nuxeo.automation.scripting.api.AutomationScriptingService;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 7.2
 */
public class AutomationScriptingServiceImpl implements AutomationScriptingService {

    protected String jsWrapper = null;

    protected String getJSWrapper(boolean refresh) throws OperationException {
        if (jsWrapper == null || refresh) {
            StringBuffer sb = new StringBuffer();
            AutomationService as = Framework.getService(AutomationService.class);
            Map<String, List<String>> opMap = new HashMap<>();
            List<String> flatOps = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            for (OperationType op : as.getOperations()) {
                if (op.getDocumentation().isChain()) {
                    continue;
                }
                ids.add(op.getId());
                if (op.getAliases() != null) {
                    Collections.addAll(ids, op.getAliases());
                }
            }
            // Create js object related to operation categories
            for (String id : ids) {
                parseAutomationIDSForScripting(opMap, flatOps, id);
            }
            for (String obName : opMap.keySet()) {
                List<String> ops = opMap.get(obName);
                sb.append("\nvar ").append(obName).append("={};");
                for (String opId : ops) {
                    generateFunction(sb, opId);
                }
            }
            for (String opId : flatOps) {
                generateFlatFunction(sb, opId);
            }
            jsWrapper = sb.toString();
        }
        return jsWrapper;
    }

    @Override
    public String getJSWrapper() throws OperationException {
        return getJSWrapper(false);
    }

    protected final ThreadLocal<ScriptEngine> engines = new ThreadLocal<ScriptEngine>() {
        @Override
        protected ScriptEngine initialValue() {
            return getEngine();
        }
    };

    protected ScriptEngine getEngine() {
        try {
            if (Boolean.valueOf(Framework.getProperty(AutomationScriptingConstants.AUTOMATION_SCRIPTING_PRECOMPILE,
                    AutomationScriptingConstants.DEFAULT_PRECOMPILE_STATUS))) {
                return new NashornScriptEngineFactory().getScriptEngine(AutomationScriptingConstants.NASHORN_OPTIONS);
            } else {
                return new NashornScriptEngineFactory().getScriptEngine();
            }
        } catch (IllegalArgumentException e) {
            throw new AutomationScriptingException(
                    "Cannot create Nashorn Engine. Make sure you're running Nuxeo with jdk8u25 at least.", e);
        }
    }

    @Override
    public void run(InputStream in, CoreSession session) throws ScriptException, OperationException {
        try {
            run(IOUtils.toString(in, "UTF-8"), session);
        } catch (IOException e) {
            throw new AutomationScriptingException(e);
        }
    }

    @Override
    public void run(String script, CoreSession session) throws ScriptException, OperationException {
        ScriptEngine engine = engines.get();
        engine.setContext(new SimpleScriptContext());
        engine.eval(getJSWrapper());
        engine.put(AutomationScriptingConstants.AUTOMATION_MAPPER_KEY, new AutomationMapper(session));
        engine.eval(script);
    }

    @Override
    public <T> T getInterface(Class<T> scriptingOperationInterface, String script, CoreSession session)
            throws ScriptException, OperationException {
        run(script, session);
        Invocable inv = (Invocable) engines.get();
        return inv.getInterface(scriptingOperationInterface);
    }

    protected void parseAutomationIDSForScripting(Map<String, List<String>> opMap, List<String> flatOps, String id) {
        if (id.split("\\.").length > 2) {
            return;
        }
        int idx = id.indexOf(".");
        if (idx > 0) {
            String obName = id.substring(0, idx);
            List<String> ops = opMap.get(obName);
            if (ops == null) {
                ops = new ArrayList<>();
            }
            ops.add(id);
            opMap.put(obName, ops);
        } else {
            // Flat operation: no need of category
            flatOps.add(id);
        }
    }

    protected void generateFunction(StringBuffer sb, String opId) {
        sb.append("\n" + opId + " = function(input,params) {");
        sb.append("\nreturn automation.executeOperation('" + opId + "', input , params);");
        sb.append("\n};");
    }

    protected void generateFlatFunction(StringBuffer sb, String opId) {
        sb.append("\nvar " + opId + " = function(input,params) {");
        sb.append("\nreturn automation.executeOperation('" + opId + "', input , params);");
        sb.append("\n};");
    }
}
