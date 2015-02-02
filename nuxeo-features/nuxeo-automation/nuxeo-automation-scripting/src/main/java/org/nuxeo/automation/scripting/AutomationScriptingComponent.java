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
package org.nuxeo.automation.scripting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.automation.scripting.operation.ScriptingOperationDescriptor;
import org.nuxeo.automation.scripting.operation.ScriptingTypeImpl;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 7.2
 */
public class AutomationScriptingComponent extends DefaultComponent implements AutomationScriptingService {

    private static final Log log = LogFactory.getLog(AutomationScriptingComponent.class);

    protected ScriptEngineManager engineManager;

    protected Compilable compiler;

    protected String jsWrapper = null;

    protected CompiledScript compiledJSWrapper = null;

    protected static final boolean preCompile = false;

    public static final String EP_OP = "operation";

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        engineManager = new ScriptEngineManager();
        if (preCompile) {
            compiler = (Compilable) engineManager.getEngineByName("Nashorn");
        }
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (EP_OP.equals(extensionPoint)) {
            ScriptingOperationDescriptor desc = (ScriptingOperationDescriptor) contribution;
            AutomationService as = Framework.getService(AutomationService.class);
            ScriptingTypeImpl type = new ScriptingTypeImpl(as, desc);
            try {
                as.putOperation(type, true);
            } catch (OperationException e) {
                throw new AutomationScriptingException(e);
            }
        } else {
            super.registerContribution(contribution, extensionPoint, contributor);
        }
    }

    public String getJSWrapper() {
        return getJSWrapper(false);
    }

    public synchronized CompiledScript getCompiledJSWrapper() throws ScriptException {
        if (compiledJSWrapper == null) {
            String script = getJSWrapper(false);
            compiledJSWrapper = compiler.compile(script);
        }
        return compiledJSWrapper;
    }

    public synchronized String getJSWrapper(boolean refresh) {
        if (jsWrapper == null || refresh) {
            StringBuffer sb = new StringBuffer();
            AutomationService as = Framework.getService(AutomationService.class);
            Map<String, List<String>> opMap = new HashMap<>();
            List<String> flatOps = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            for (OperationType op : as.getOperations()) {
                ids.add(op.getId());
                if (op.getAliases() != null) {
                    Collections.addAll(ids, op.getAliases());
                }
            }
            // Create js object related to operation categories
            for(String id: ids){
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
                generateFunction(sb, opId);
            }
            jsWrapper = sb.toString();
        }
        return jsWrapper;
    }

    protected void parseAutomationIDSForScripting(Map<String, List<String>> opMap, List<String> flatOps, String id) {
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

    public ScriptRunner getRunner(CoreSession session) throws ScriptException {
        ScriptRunner runner = getRunner();
        runner.setCoreSession(session);
        return runner;
    }

    public ScriptRunner getRunner() throws ScriptException {
        ScriptRunner runner;
        if (preCompile) {
            runner = new ScriptRunner(engineManager, getCompiledJSWrapper());
        } else {
            runner = new ScriptRunner(engineManager, getJSWrapper());
        }
        return runner;
    }

}
