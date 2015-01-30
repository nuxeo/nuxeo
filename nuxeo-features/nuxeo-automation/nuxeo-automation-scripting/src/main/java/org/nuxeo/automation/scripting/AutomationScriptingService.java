package org.nuxeo.automation.scripting;

import javax.script.ScriptException;

import org.nuxeo.ecm.core.api.CoreSession;

public interface AutomationScriptingService {

    String getJSWrapper();

    String getJSWrapper(boolean refresh);

    ScriptRunner getRunner(CoreSession session) throws ScriptException;

    ScriptRunner getRunner() throws ScriptException;
}
