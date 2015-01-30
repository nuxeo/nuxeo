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

    protected int nbFunctions = 0;

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
                // TODO: move putting operation elsewhere
                log.error(e);
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
            nbFunctions = 0;
            StringBuffer sb = new StringBuffer();

            AutomationService as = Framework.getService(AutomationService.class);

            Map<String, List<OperationType>> opMap = new HashMap<String, List<OperationType>>();
            List<OperationType> flatOps = new ArrayList<>();

            for (OperationType op : as.getOperations()) {
                String id = op.getId();
                int idx = id.indexOf(".");
                if (idx > 0) {
                    String obName = id.substring(0, idx);
                    List<OperationType> ops = opMap.get(obName);
                    if (ops == null) {
                        ops = new ArrayList<>();
                    }
                    ops.add(op);
                    opMap.put(obName, ops);
                } else {
                    flatOps.add(op);
                }
            }

            for (String obName : opMap.keySet()) {
                List<OperationType> ops = opMap.get(obName);
                sb.append("\nvar " + obName + "={};");

                for (OperationType op : ops) {
                    generateFunction(sb, op);
                }
            }
            for (OperationType op : flatOps) {
                generateFunction(sb, op);
            }

            jsWrapper = sb.toString();

        }
        return jsWrapper;
    }

    protected void generateFunction(StringBuffer sb, OperationType op) {
        sb.append("\n" + op.getId() + " = function(input,params) {");
        sb.append("\nreturn automation.executeOperation('" + op.getId() + "', input , params);");
        sb.append("\n};");
        nbFunctions++;
    }

    public ScriptRunner getRunner(CoreSession session) throws ScriptException {
        ScriptRunner runner = getRunner();
        runner.setCoreSession(session);
        return runner;
    }

    public ScriptRunner getRunner() throws ScriptException {
        ScriptRunner runner = null;
        if (!preCompile) {
            runner = new ScriptRunner(engineManager, getJSWrapper());
        } else {
            runner = new ScriptRunner(engineManager, getCompiledJSWrapper());
        }
        return runner;
    }

}
