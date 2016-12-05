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

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.Map;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import org.apache.commons.io.Charsets;
import org.apache.commons.lang.StringUtils;
import org.nuxeo.automation.scripting.api.AutomationScriptingService;
import org.nuxeo.automation.scripting.api.AutomationScriptingService.Session;
import org.nuxeo.automation.scripting.internals.AutomationMapper;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;

/**
 * {@link UserMapper} implementation using Nashorn to implement logic using JavaScript
 *
 * @author tiry
 * @since 7.4
 */

public class NashornUserMapper extends AbstractUserMapper {

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
        try (CoreSession core = CoreInstance
                .openCoreSession(Framework.getService(RepositoryManager.class).getDefaultRepositoryName())) {
            try (Session session = Framework.getService(AutomationScriptingService.class).get(core)) {
                Map<String, Object> bindings = session.adapt(ScriptEngine.class)
                        .getBindings(ScriptContext.ENGINE_SCOPE);
                bindings.put("nuxeoPrincipal", principal);
                bindings.put("userObject", userObject);
                bindings.put("params", params);
                session.run(new ByteArrayInputStream(wrapperSource.getBytes(Charsets.UTF_8)));
                return bindings.get("userObject");
            } catch (Exception e) {
                throw new NuxeoException("Error while executing JavaScript mapper", e);
            }
        }
    }

    @Override
    public void init(Map<String, String> params) throws Exception {

    }

    @Override
    public void release() {
        // NOP
    }

    @Override
    protected void resolveAttributes(Object userObject, Map<String, Serializable> searchAttributes,
            Map<String, Serializable> userAttributes, Map<String, Serializable> profileAttributes) {

        try (CoreSession core = CoreInstance
                .openCoreSession(Framework.getService(RepositoryManager.class).getDefaultRepositoryName())) {
            try (Session session = Framework.getService(AutomationScriptingService.class).get(core)) {
                AutomationMapper mapper = session.adapt(AutomationMapper.class);
                mapper.put("searchAttributes", searchAttributes);
                mapper.put("profileAttributes", profileAttributes);
                mapper.put("userAttributes", userAttributes);
                mapper.put("userObject", userObject);
                session.run(new ByteArrayInputStream(mapperSource.getBytes(Charsets.UTF_8)));
            } catch (Exception e) {
                throw new NuxeoException("Error while executing JavaScript mapper", e);
            }
        }

    }

}
