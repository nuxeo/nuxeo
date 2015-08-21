package org.nuxeo.usermapper.extension;

import java.io.Serializable;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.automation.scripting.internals.ScriptingCache;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

public class NashornUserMapper extends AbstractUserMapper {

    protected static final Log log = LogFactory.getLog(NashornUserMapper.class);

    protected ScriptEngine engine;

    protected final String scriptSource;

    public NashornUserMapper(String script) {
        super();
        scriptSource = script;
    }

    @Override
    public Object wrapNuxeoPrincipal(NuxeoPrincipal principal) {
        throw new UnsupportedOperationException("The JavaScript mapper does not support wrapping");
    }

    @Override
    public void init(Map<String, String> params) throws Exception {
        ScriptingCache scripting = new ScriptingCache(true);
        engine = scripting.getScriptEngine();
    }

    @Override
    public void release() {
        // NOP
    }

    @Override
    protected void resolveAttributes(Object userObject, Map<String, Serializable> searchAttributes,
            Map<String, Serializable> userAttributes, Map<String, Serializable> profileAttributes) {
        Bindings bindings = new SimpleBindings();
        bindings.put("searchAttributes", searchAttributes);
        bindings.put("profileAttributes", profileAttributes);
        bindings.put("userAttributes", userAttributes);
        bindings.put("userObject", userObject);

        try {
            engine.eval(scriptSource, bindings);
        } catch (ScriptException e) {
            log.error("Error while executing JavaScript mapper", e);
        }
    }

}
