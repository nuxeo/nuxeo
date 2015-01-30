package org.nuxeo.automation.scripting.operation;

import java.util.List;
import java.util.Map;

import javax.script.ScriptException;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import org.nuxeo.automation.scripting.AutomationScriptingService;
import org.nuxeo.automation.scripting.MarshalingHelper;
import org.nuxeo.automation.scripting.ScriptRunner;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.runtime.api.Framework;

public class ScriptingOperationImpl {

    protected final ScriptRunner runner;

    protected final OperationContext ctx;

    protected final Map<String, Object> args;

    protected final String source;

    public ScriptingOperationImpl(String source, OperationContext ctx, Map<String, Object> args) throws ScriptException {
        AutomationScriptingService ass = Framework.getService(AutomationScriptingService.class);
        runner = ass.getRunner();
        runner.setCoreSession(ctx.getCoreSession());
        this.ctx = ctx;
        this.args = args;
        this.source = source;
    }

    public Object run(Object input) throws Exception {
        try {
            ScriptingOperationInterface itf = runner.getInterface(ScriptingOperationInterface.class, source);
            return wrapResult(itf.run(ctx.getVars(), input, args));
        } catch (ScriptException e) {
            throw new OperationException(e);
        }

    }

    protected Object wrapResult(Object res) {
        if (res == null) {
            return null;
        }
        if (res instanceof ScriptObjectMirror) {
            Object unwrapped =  MarshalingHelper.unwrap((ScriptObjectMirror)res);
            
            if (unwrapped instanceof List<?>) {
                DocumentModelList docs = new DocumentModelListImpl();
                List<?> l = (List<?>) unwrapped;
                for (Object item : l) {
                    if (item instanceof DocumentModel) {
                        docs.add((DocumentModel)item);
                    }
                }
                if (docs.size()==l.size() && docs.size()>0) {
                    return docs;
                }            
            }            
            return unwrapped;
        }
        return res;
        
    }
/*
    protected ScriptableMap wrap(OperationContext ctx) {
        return wrap(ctx.getVars());
    }

    protected ScriptableMap wrap(Map<String, Object> vars) {
        return new ScriptableMap(vars);
    }

    protected NativeObject wrap2(OperationContext ctx) {
        return wrap2(ctx.getVars());
    }

    protected NativeObject wrap2(Map<String, Object> vars) {
        NativeObject no = new NativeObject();
        for (String k : vars.keySet()) {
            no.defineProperty(k, vars.get(k), NativeObject.READONLY);
        }
        return no;
    }
*/
}
