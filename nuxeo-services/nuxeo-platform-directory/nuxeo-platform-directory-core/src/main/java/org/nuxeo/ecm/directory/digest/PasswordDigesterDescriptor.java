/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.directory.digest;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 *
 *
 * @since 7.1
 */
@XObject("digester")
public class PasswordDigesterDescriptor {

    @XNode("@name")
    String name;


    @XNode("@enabled")
    boolean enabled = true;


    @XNode("@class")
    Class<PasswordDigester> digesterKlass;

    @XNodeMap(value = "params/param", key = "@name", type = HashMap.class, componentType = String.class)
    Map<String, String> params;

    public PasswordDigester buildDigester() {
        try {
            PasswordDigester digester = digesterKlass.newInstance();
            digester.setName(name);
            digester.setParams(params);
            return digester;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Unable to build digester " + name);
        }
    }

    @Override
    public String toString() {
        return String.format("Digester(%s)[class=%s,enabled=%s]", name, digesterKlass.getName(), enabled ? "true" : "false");
    }

}
