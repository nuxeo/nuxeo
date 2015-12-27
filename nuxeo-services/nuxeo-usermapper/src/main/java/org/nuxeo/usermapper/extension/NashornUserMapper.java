/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.usermapper.extension;

import java.io.Serializable;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.automation.scripting.internals.ScriptingCache;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * {@link UserMapper} implementation using Nashorn to implement logic using JavaScript
 *
 * @author tiry
 * @since 7.4
 */

public class NashornUserMapper extends AbstractUserMapper {

    protected ScriptEngine engine;

    protected final String mapperSource;

    protected final String wrapperSource;

    public NashornUserMapper(String mapperScript, String wrapperScript) {
        super();
        mapperSource = mapperScript;
        wrapperSource = wrapperScript;
    }

    @Override
    public Object wrapNuxeoPrincipal(NuxeoPrincipal principal, Object userObject, Map<String, Serializable> params) {
        if (StringUtils.isEmpty(wrapperSource)) {
            return null;
        }
        Bindings bindings = new SimpleBindings();
        bindings.put("nuxeoPrincipal", principal);
        bindings.put("userObject", userObject);
        bindings.put("params", params);
        try {
            engine.eval(wrapperSource, bindings);
        } catch (ScriptException e) {
            log.error("Error while executing JavaScript mapper", e);
        }
        return bindings.get("userObject");
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
            engine.eval(mapperSource, bindings);
        } catch (ScriptException e) {
            log.error("Error while executing JavaScript mapper", e);
        }
    }

}
