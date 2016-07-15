/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     tiry
 */
package org.nuxeo.ecm.core.event.pipe.dispatch;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * XMap Descriptor for contributing a new {@link EventBundleDispatcher}
 *
 * @since 8.4
 */
@XObject("eventDispatcher")
public class EventDispatcherDescriptor {

    public static final Log log = LogFactory.getLog(EventDispatcherDescriptor.class);

    public EventDispatcherDescriptor() {
    };

    public EventDispatcherDescriptor(String name, Class<? extends EventBundleDispatcher> clazz) {
        this.name = name;
        this.clazz = clazz;
    }

    @XNode("@name")
    protected String name;

    @XNodeMap(value = "parameters/parameter", key = "@name", type = HashMap.class, componentType = String.class)
    Map<String, String> parameters = new HashMap<String, String>();

    public String getName() {
        return name == null ? clazz.getName() : name;
    }

    /**
     * The implementation class.
     */
    @XNode("@class")
    protected Class<? extends EventBundleDispatcher> clazz;

    public Map<String, String> getParameters() {
        return parameters;
    }

    public EventBundleDispatcher getInstance() {
        try {
            return clazz.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public EventDispatcherDescriptor clone() {
        EventDispatcherDescriptor copy = new EventDispatcherDescriptor(name, clazz);
        copy.parameters = new HashMap<String, String>(parameters);
        return copy;
    }
}
