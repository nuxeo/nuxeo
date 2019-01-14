/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     tiry
 */
package org.nuxeo.ecm.core.event.pipe.dispatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

import java.util.HashMap;
import java.util.Map;

/**
 * XMap Descriptor for contributing a new {@link EventBundleDispatcher}
 *
 * @since 8.4
 */
@XObject("eventDispatcher")
public class EventDispatcherDescriptor {

    public static final Log log = LogFactory.getLog(EventDispatcherDescriptor.class);

    public EventDispatcherDescriptor() {
    }

    public EventDispatcherDescriptor(String name, Class<? extends EventBundleDispatcher> clazz) {
        this.name = name;
        this.clazz = clazz;
    }

    @XNode("@name")
    protected String name;

    @XNodeMap(value = "parameters/parameter", key = "@name", type = HashMap.class, componentType = String.class)
    Map<String, String> parameters = new HashMap<>();

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
            return clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public EventDispatcherDescriptor clone() {
        EventDispatcherDescriptor copy = new EventDispatcherDescriptor(name, clazz);
        copy.parameters = new HashMap<>(parameters);
        return copy;
    }
}
