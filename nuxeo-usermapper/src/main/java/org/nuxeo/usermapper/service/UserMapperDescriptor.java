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

    @XNode("script")
    protected String sourceScript;

    public UserMapper getInstance() throws Exception {
        UserMapper mapper = null;

        if (getType().equals(Type.none)) {
            throw new NuxeoException("Mapper has an unknown type");
        }

        if (getType().equals(Type.java)) {
            if (mapperClass==null) {
                throw new NuxeoException("Java Mapper must provide an implementation class ");
            }
            mapper = mapperClass.newInstance();
        }
        else if (getType().equals(Type.groovy)) {
            mapper = new GroovyUserMapper(sourceScript);
        }
        else if (getType().equals(Type.javascript)) {
            mapper = new NashornUserMapper(sourceScript);
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
