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

package org.nuxeo.usermapper.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.usermapper.extension.GroovyUserMapper;
import org.nuxeo.usermapper.extension.NashornUserMapper;
import org.nuxeo.usermapper.extension.UserMapper;

/**
 * XMap descriptor for contributing {@link UserMapper} plugins
 *
 * @author tiry
 * @since 7.4
 */
@XObject("mapper")
public class UserMapperDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Type {
        java, groovy, javascript, none
    }

    @XNode("@name")
    protected String name;

    @XNode("@type")
    protected String type;

    @XNode("@class")
    Class<UserMapper> mapperClass;

    @XNodeMap(value = "parameters/parameter", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> params;

    @XNode("mapperScript")
    protected String mapperScript;

    @XNode("wrapperScript")
    protected String wrapperScript;

    public UserMapper getInstance() throws Exception {
        UserMapper mapper = null;
        switch (getType()) {
        case java:
            if (mapperClass == null) {
                throw new NuxeoException("Java Mapper must provide an implementation class ");
            }
            mapper = mapperClass.getDeclaredConstructor().newInstance();
            break;
        case groovy:
            mapper = new GroovyUserMapper(mapperScript, wrapperScript);
            break;
        case javascript:
            mapper = new NashornUserMapper(mapperScript, wrapperScript);
            break;
        case none:
            // fall-through
        default:
            throw new NuxeoException("Mapper has an unknown type");
        }
        // run init
        mapper.init(params);
        return mapper;
    }

    public Type getType() {
        if (type == null) {
            if (mapperClass != null) {
                return Type.java;
            }
        }
        if ("java".equalsIgnoreCase(type)) {
            return Type.java;
        }
        if ("groovy".equalsIgnoreCase(type)) {
            return Type.groovy;
        }
        if ("javascript".equalsIgnoreCase(type)) {
            return Type.javascript;
        }
        if ("js".equalsIgnoreCase(type)) {
            return Type.javascript;
        }
        return Type.none;
    }
}
