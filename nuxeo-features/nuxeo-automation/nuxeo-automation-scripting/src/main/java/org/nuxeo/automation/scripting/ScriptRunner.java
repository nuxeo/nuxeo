package org.nuxeo.automation.scripting;

import java.io.InputStream;

import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.io.IOUtils;
import org.nuxeo.ecm.core.api.CoreSession;

public class ScriptRunner {

    protected final ScriptEngineManager engineManager;

    protected final ScriptEngine engine;

    protected String jsBinding;

    protected CompiledScript compiledJSWrapper;

    protected boolean initDone = false;

    protected CoreSession session;

    public ScriptRunner(ScriptEngineManager engineManager, String jsBinding) {
        this.engineManager = engineManager;
        engine = engineManager.getEngineByName("Nashorn");
        this.jsBinding = jsBinding;
    }

    public ScriptRunner(ScriptEngineManager engineManager, CompiledScript jsBinding) {
        this.engineManager = engineManager;
        engine = engineManager.getEngineByName("Nashorn");
        this.compiledJSWrapper = jsBinding;
    }

    public long initialize() throws ScriptException {
        if (!initDone) {
            long t0 = System.currentTimeMillis();
            if (compiledJSWrapper != null) {
                compiledJSWrapper.eval(engine.getContext());
            } else {
                engine.eval(jsBinding);
            }
            initDone = true;
            return System.currentTimeMillis() - t0;
        } else {
            return 0;
        }
    }

    public void run(InputStream in) throws Exception {
        run(IOUtils.toString(in, "UTF-8"));
    }

    public void run(String script) throws ScriptException {
        initialize();
        engine.put("automation", new AutomationMapper(session));
        StringBuffer nameSpacedJS = new StringBuffer();
        nameSpacedJS.append("(function(){");
        nameSpacedJS.append(script);
        nameSpacedJS.append("})();");
        engine.eval(nameSpacedJS.toString());
    }

    public void setCoreSession(CoreSession session) {
        this.session = session;
    }

    public <T> T getInterface(Class<T> javaInterface, String script) throws Exception {
        initialize();
        engine.put("automation", new AutomationMapper(session));
        engine.eval(script);
        Invocable inv = (Invocable) engine;
        return inv.getInterface(javaInterface);
    }

    public Invocable getInvocable() {
        return (Invocable) engine;
    }

}
