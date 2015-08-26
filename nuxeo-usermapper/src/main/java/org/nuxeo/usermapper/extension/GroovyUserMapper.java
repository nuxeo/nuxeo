/*
 * (C) Copyright 2006-2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.usermapper.extension;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * Implement the {@link UserMapper} using Groovy Scripting for the mapping part
 *
 * @author tiry
 *
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
    protected void resolveAttributes(Object userObject,
            Map<String, Serializable> searchAttributes,
            Map<String, Serializable> userAttributes, Map<String, Serializable> profileAttributes) {

        Map<String, Object> context = new HashMap<String, Object>();
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

        Map<String, Object> context = new HashMap<String, Object>();
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
