/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;

/**
 * Implement the {@link UserMapper} using Groovy Scripting for the mapping part
 *
 * @author tiry
 * @since 7.4
 */
public class GroovyUserMapper extends AbstractUserMapper {

    protected final String mapperSource;

    protected final String wrapperSource;

    protected GroovyClassLoader loader;

    protected Class<?> mapperClass;

    protected Class<?> wrapperClass;

    public GroovyUserMapper(String mapperScript, String wrapperScript) {
        super();
        mapperSource = mapperScript;
        wrapperSource = wrapperScript;
    }

    @Override
    public void init(Map<String, String> params) {
        loader = new GroovyClassLoader(this.getClass().getClassLoader());
        mapperClass = loader.parseClass(mapperSource);
        if (!StringUtils.isEmpty(wrapperSource)) {
            wrapperClass = loader.parseClass(wrapperSource);
        }
    }

    @Override
    protected void resolveAttributes(Object userObject, Map<String, Serializable> searchAttributes,
            Map<String, Serializable> userAttributes, Map<String, Serializable> profileAttributes) {
        Map<String, Object> context = new HashMap<>();
        context.put("searchAttributes", searchAttributes);
        context.put("profileAttributes", profileAttributes);
        context.put("userAttributes", userAttributes);
        context.put("userObject", userObject);
        Binding binding = new Binding(context);
        Script script = InvokerHelper.createScript(mapperClass, binding);
        script.run();
    }

    @Override
    public Object wrapNuxeoPrincipal(NuxeoPrincipal principal, Object userObject, Map<String, Serializable> params) {
        Map<String, Object> context = new HashMap<>();
        context.put("nuxeoPrincipal", principal);
        context.put("userObject", userObject);
        context.put("params", params);
        Binding binding = new Binding(context);
        Script script = InvokerHelper.createScript(wrapperClass, binding);
        script.run();
        return context.get("userObject");
    }

    @Override
    public void release() {
        loader.clearCache();
        try {
            loader.close();
        } catch (IOException e) {
            log.error("Error during Groovy cleanup", e);
        }
    }

}
